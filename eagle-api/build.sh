#!/usr/bin/env bash
{
    BD_DIR="$(cd $(dirname $0)/..; pwd)"
    MODULE_DIR="$BD_DIR/eagle-api"
    OUT_DIR="$MODULE_DIR/output"
    TARGET_DIR="$MODULE_DIR/target"

    cd $MODULE_DIR
    rm -rf $OUT_DIR/*
    rm -rf $TARGET_DIR/*

    mkdir -p $OUT_DIR/lib
    mkdir -p $OUT_DIR/bin

    profile="prod"
    if [ $# -ge 1 ]; then
        if [ "$1" != "prod" -a "$1" != "test" -a "$1" != "dev" ]; then
            profile="prod"
        else
            profile="$1"
        fi
    fi

    export profile=$profile

    controller="control.sh"
    if [ $profile = "prod" ]; then
        controller="control.sh"
    fi

    #start build
    echo "Start building ......"
    cd $BD_DIR
    mvn -pl eagle-api -am package -Dmaven.test.skip=true -P$profile&&
    mv $MODULE_DIR/target/*.jar $OUT_DIR/lib/ &&
    cp $MODULE_DIR/$controller $OUT_DIR/control.sh &&
    cp $MODULE_DIR/setenv.sh $OUT_DIR/bin/ &&
    ls $OUT_DIR/ &&
    exit 0
} || {
    echo "build failed!"
    exit 1
}