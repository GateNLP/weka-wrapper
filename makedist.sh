#!/bin/bash

name=weka-wrapper
tmpdir=/tmp
curdir=`pwd -P`
version=`perl -n -e 'if (/<version>([^<]+)<\/version>.+makedist/) { print $1;}' < $curdir/pom.xml`
destdir=$tmpdir/${name}$$
curbranch=`git branch | grep '\*' | cut -c 3-`
echo Making a release zip for $name, version $version from branch $curbranch
rm -rf "$destdir"
mkdir -p $destdir/$name
rm -f $name-*.zip
rm -f $name-*.tgz
git archive --format zip --output ${name}-${version}-src.zip --prefix=$name/ $curbranch
pushd $destdir
unzip $curdir/${name}-${version}-src.zip
cd $name
mvn clean
mvn dependency:copy-dependencies 
mvn package
rm -rf target/classes
rm -rf target/maven-*
rm -rf makedist.sh
rm -rf tests
rm $curdir/${name}-${version}-src.zip
cd ..
zip -r $curdir/$name-$version.zip $name
echo Created a release zip for $name, version $version from branch $curbranch
echo Zip file is $curdir/$name-$version.zip
popd >& /dev/null
