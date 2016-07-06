#!/usr/bin/env bash

_sem_ver="$(cat version.txt | xargs)"
_commits="$(git rev-list --count HEAD)"
_hash="$(git rev-parse --short HEAD)"

if [ $# -eq 0 ]; then
    if [ "${CIRCLE_PROJECT_USERNAME:-}" = "pico-works" ]; then
        if [ "${CIRCLE_TAG:-}" = "v${_sem_ver}" ]; then
            echo "$_sem_ver"
            exit 0
        fi

        if [ "${CIRCLE_BRANCH:-}" = "develop" ]; then
            echo "$_sem_ver-$_commits"
            exit 0
        fi
    fi

    echo "$_sem_ver-$_commits-$_hash"
else
    case "$1" in
    tag)
        _untagged_deps="$(cat project/Build.scala  | grep val | grep 'org.pico' | grep '[0-9]-[0-9]')"

        if [ "$(git rev-parse --abbrev-ref HEAD)" != "develop" ]; then
            echo "Cannot tag from branch other than develop. To force, type:"
            echo ""
            echo "    git tag -a \"v${_sem_ver}\" -m \"New version ${_sem_ver}\" -f"
            echo "    git push up "v${_sem_ver}" -f"
            echo ""
            exit 1
        fi

        git fetch --all
        
        _local=$(git rev-parse @)
        _remote=$(git rev-parse up/develop)

        if [ "$_local" != "$_remote" ]; then
            echo "Cannot tag from out-of-date develop. To force, type:"
            echo ""
            echo "    git tag -a \"v${_sem_ver}\" -m \"New version ${_sem_ver}\" -f"
            echo "    git push up "v${_sem_ver}" -f"
            echo ""
            exit 1
        fi

        if [ "$(git rev-parse --abbrev-ref HEAD)" != "develop" ]; then
            echo "Must be on develop branch to tag"
            exit 1
        fi

        if [ ! -z "$_untagged_deps" ]; then
            echo "Cannot tag because untagged dependencies exist. To force, type:"
            echo ""
            echo "    git tag -a \"v${_sem_ver}\" -m \"New version ${_sem_ver}\" -f"
            echo "    git push up "v${_sem_ver}" -f"
            echo ""
            echo "$_untagged_deps"
            echo ""
            exit 1
        fi

        git tag -a "v${_sem_ver}" -m "New version ${_sem_ver}" && git push up "v${_sem_ver}" || {
            echo "Tagging failed.  To force, type:"
            echo ""
            echo "    git tag -a \"v${_sem_ver}\" -m \"New version ${_sem_ver}\" -f"
            echo "    git push up "v${_sem_ver}" -f"
            echo ""
            exit 1
        }
        exit 0
        ;;
    new-version-branch)
        git checkout -b "PR-new-version-$(git rev-parse --short @)"
        exit 0
        ;;
    esac
fi

echo "$_sem_ver-$_commits-$_hash"
