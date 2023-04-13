
BUILD_TARGET=$1
BUILD_TARGET_SDK=
BUILD_TARGET_REGEX="java$1"

case $1 in
	8)
		BUILD_TARGET_SDK=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home 
		;;

	11) 
		BUILD_TARGET_SDK=/Library/Java/JavaVirtualMachines/jdk-11.0.17.jdk/Contents/Home
		;;

	17) 
		BUILD_TARGET_SDK=/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home
		;;
esac

echo $BUILD_TARGET
echo $BUILD_TARGET_SDK

BUILD_FILE_LIST=()
for BUILD_FILE in `find src -regex '.*_[0-9]*\.java[0-9]*'`; do
	# get file info
	FILE=`echo $BUILD_FILE | sed -E 's/(.*)_([0-9]+)\.java[0-9]*$/\1/'`
	FILE_VER=`echo $BUILD_FILE | sed -E 's/.*_([0-9]+)\.java[0-9]*$/\1/'`
	# change to disable first.
	BUILD_FILE_TARGET=`echo $BUILD_FILE | sed -E 's/_([0-9]+)\.java[0-9]*$/_\1.java\1/'`
	echo "rename $BUILD_FILE"
	mv "$BUILD_FILE" "$BUILD_FILE_TARGET"
	if [[ $FILE_VER -gt $BUILD_TARGET ]]; then
		continue
	fi
	# calculate the version information
	for INDEX in ${!BUILD_FILE_LIST[@]}; do 
		IT="${BUILD_FILE_LIST[$INDEX]}"
		if [[ "${IT}" == "${FILE}_"* ]]; then
			FILE=""
			IT_VER=`echo $IT | sed -E 's/.*_([0-9]+)\.java[0-9]*$/\1/'`
			if [[ $FILE_VER -ge $IT_VER ]]; then
			  BUILD_FILE_LIST[$INDEX]=$BUILD_FILE_TARGET
			fi
			break
		fi
	done
	if [[ "$FILE" != "" ]]; then
		BUILD_FILE_LIST+=($BUILD_FILE_TARGET)
	fi
done

for BUILD_FILE in ${BUILD_FILE_LIST[@]}; do
	BUILD_FILE_TARGET=`echo $BUILD_FILE | sed -E 's/_([0-9]+)\.java[0-9]*$/_\1.java/'`
	mv "$BUILD_FILE" "$BUILD_FILE_TARGET"
done

COMPILED_RESOURCES_DIR="src/main/resources"
COMPILED_CLASS_DIR="$COMPILED_RESOURCES_DIR/manifold/internal/javac-$BUILD_TARGET/"

rm -rf "$COMPILED_CLASS_DIR"
mkdir -p "$COMPILED_CLASS_DIR"

export JAVA_HOME=$BUILD_TARGET_SDK
mv "$COMPILED_RESOURCES_DIR" "$COMPILED_RESOURCES_DIR.tmp"
mvn clean compile
mv "$COMPILED_RESOURCES_DIR.tmp" "$COMPILED_RESOURCES_DIR"

for COMPILED_FILE in `find target -regex '.*_[0-9]*.*\.class.*'`; do
	echo copy $COMPILED_FILE
	cp "$COMPILED_FILE" "$COMPILED_CLASS_DIR"
done
