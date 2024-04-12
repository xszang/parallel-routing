fn=cufr
jar cvfm ${fn}.jar MANIFEST.MF -C ../bin/ com
if [ -d ${fn} ]
then
    echo "${fn} exists, remove it."
    rm -rf ${fn}
fi
mkdir ${fn}
cp -r ../jars ${fn}
mv ${fn}.jar ${fn}
native-image -jar ${fn}/${fn}.jar ${fn}/${fn}
chmod -R 777 ${fn}
#zip -q -r ${fn}.zip ${fn}
#chmod 755 ${fn}.zip
