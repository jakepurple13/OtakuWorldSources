#!/bin/bash
#set -e

TOOLS="$(ls -d ${ANDROID_HOME}/build-tools/* | tail -1)"

mkdir -p repo/apk
mkdir -p repo/icon

cp -f apk/* repo/apk

cd repo

APKS=( ../apk/*".apk" )

echo "Creating Repo! Please wait..."

for APK in ${APKS[@]}; do
    FILENAME=$(basename ${APK})
    BADGING="$(${TOOLS}/aapt dump --include-meta-data badging $APK)"

    #FEATURE=$(echo "$BADGING" | grep -oE "uses-feature: name=\'programmersbox\.otaku\.extension\.(.*?)'")
    FEATURE=$(echo "$BADGING" | grep -oE "uses-feature: name=\'programmersbox\.otaku\.extension\.[^']*'" | rev | cut -d '.' -f 1 | rev | sed "s/'$//")

    PACKAGE=$(echo "$BADGING" | grep package:)
    PKGNAME=$(echo $PACKAGE | grep -Po "package: name='\K[^']+")
    VCODE=$(echo $PACKAGE | grep -Po "versionCode='\K[^']+")
    VNAME=$(echo $PACKAGE | grep -Po "versionName='\K[^']+")

    APPLICATION=$(echo "$BADGING" | grep application:)
    LABEL=$(echo $APPLICATION | grep -Po "label='\K[^']+")

    LANG="en"#$(echo $APK | grep -Po "otakuworld-\K[^\.]+")

    #ICON=$(echo "$BADGING" | grep -Po "application-icon-320.*'\K[^']+")
    #unzip -p $APK $ICON > icon/${PKGNAME}.png

    # TODO: legacy icons; remove after a while
    #cp icon/${PKGNAME}.png icon/${FILENAME%.*}.png

    SOURCE_INFO=$(jq ".[\"$PKGNAME\"]" < ../output.json)

    # Fixes the language code without needing to update the packages.
    SOURCE_LEN=$(echo $SOURCE_INFO | jq length)

    if [ $SOURCE_LEN = "1" ]; then
        SOURCE_LANG=$(echo $SOURCE_INFO | jq -r '.[0].lang')

        if [ $SOURCE_LANG != $LANG ] && [ $SOURCE_LANG != "all" ] && [ $SOURCE_LANG != "other" ] && [ $LANG != "all" ] && [ $LANG != "other" ]; then
            LANG=$SOURCE_LANG
        fi
    fi

    jq -n \
        --arg name "$LABEL" \
        --arg pkg "$PKGNAME" \
        --arg apk "$FILENAME" \
        --arg lang "$LANG" \
        --argjson code $VCODE \
        --arg version "$VNAME" \
        --arg feature "$FEATURE" \
        --argjson sources "$SOURCE_INFO" \
        '{name:$name, pkg:$pkg, apk:$apk, lang:$lang, code:$code, version:$version, feature:$feature, sources:$sources}'

done | jq -sr '[.[]]' > index.json

# Alternate minified copy
jq -c '.' < index.json > index.min.json

cat index.json