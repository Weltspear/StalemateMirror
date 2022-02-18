#
# Stalemate Game
# Copyright (C) 2022 Weltspear
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

echo Downloading libraries...
mkdir lib
curl https://repo1.maven.org/maven2/org/jetbrains/annotations/22.0.0/annotations-22.0.0.jar --output lib/annotations.jar
curl https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.13.1/jackson-databind-2.13.1.jar --output lib/jackson-databind.jar
curl https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.13.1/jackson-core-2.13.1.jar --output lib/jackson-core.jar
curl https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.13.1/jackson-annotations-2.13.1.jar --output lib/jackson-annotations.jar
curl https://repo1.maven.org/maven2/com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.13.1/jackson-dataformat-yaml-2.13.1.jar --output lib/jackson-dataformat-yaml.jar
curl https://repo1.maven.org/maven2/org/yaml/snakeyaml/1.29/snakeyaml-1.29.jar --output lib/snakeyaml.jar
curl https://gitlab.com/weltspear/panic/uploads/4b90c96f75ac312bf93c0ed8a58bec3e/panic-v0.1.jar --output lib/panic.jar
echo Done!

