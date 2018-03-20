#!/bin/sh

targetRepo="$2"

remoteRefs=""

while read localRef localSha remoteRef remoteSha
do
    if [[ $remoteRef == "refs/heads/rr/"* || $remoteRef == "refs/heads/rrr/"* ]]; then
        continue
    fi

    if [ -z $remoteRefs ]; then
        remoteRefs="$remoteRef"
    else
        remoteRefs="$remoteRefs,$remoteRef"
    fi
done

if [[ -z $remoteRefs ]]; then
    exit 0
fi

echo $remoteRefs

mkdir -p ./build/prePushHook
javac -d ./build/prePushHook ./libraries/tools/kotlin-prepush-hook/src/KotlinPrePushHook.java
cd ./build/prePushHook

java KotlinPrePushHook $remoteRefs $targetRepo
returnCode=$?

cd ../..

exit $returnCode