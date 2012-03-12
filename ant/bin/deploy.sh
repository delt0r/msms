#!/bin/bash
# assume this is run in the top directory. 
zip -r $1.zip msms
cp msms/lib/msms.jar $1.jar
sed -e "s/msms\.zip/$1\.zip/g" -e "s/msms\.jar/$1\.jar/g" <html/download.shtml.sed >html/download.shtml
scp $1.zip $1.jar html/*html msms/doc/*pdf mabs@upload.univie.ac.at:~/html/ewing/msms/ 

