@echo off
echo Downloading libraries...
mkdir lib
curl.exe https://repo1.maven.org/maven2/org/jetbrains/annotations/22.0.0/annotations-22.0.0.jar --output lib/annotations.jar 
curl.exe https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.1/jackson-databind-2.13.1.jar --output lib/jackson-databind.jar 
curl.exe https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.1/jackson-core-2.13.1.jar --output lib/jackson-core.jar
curl.exe https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.1/jackson-annotations-2.13.1.jar --output lib/jackson-annotations.jar 
curl.exe https://repo1.maven.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.13.1/jackson-dataformat-yaml-2.13.1.jar --output lib/jackson-dataformat-yaml.jar 
curl.exe https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.29/snakeyaml-1.29.jar --output lib/snakeyaml.jar
curl.exe https://gitlab.com/weltspear/panic/uploads/4b90c96f75ac312bf93c0ed8a58bec3e/panic-v0.1.jar --output lib/panic.jar
echo Done!
pause

