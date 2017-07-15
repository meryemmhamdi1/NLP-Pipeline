/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class SlpToolKit_NativeLexique */

#ifndef _Included_SlpToolKit_NativeLexique
#define _Included_SlpToolKit_NativeLexique
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    lookForNext
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_SlpToolKit_NativeLexique_lookForNext
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    getSize
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_getSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    exportToASCII
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_exportToASCII
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    importFromASCII
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_importFromASCII
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    loadFromFile
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_loadFromFile
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    saveFromFile
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_saveFromFile
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    normalize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_SlpToolKit_NativeLexique_normalize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    constructor
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_constructor
  (JNIEnv *, jclass);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    destructor
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_SlpToolKit_NativeLexique_destructor
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    getArbreLexico
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_getArbreLexico
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    probabilityDefined
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_probabilityDefined
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    frequencyDefined
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_frequencyDefined
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    lemmeDefined
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_lemmeDefined
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    morphoDefined
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_morphoDefined
  (JNIEnv *, jclass, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    internalGetFromIndex
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_internalGetFromIndex
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    internalLookFor
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_internalLookFor__JLjava_lang_String_2
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    internalLookFor
 * Signature: (J[J)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_internalLookFor__J_3J
  (JNIEnv *, jclass, jlong, jlongArray);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    extractGraphy
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_SlpToolKit_NativeLexique_extractGraphy
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    extractSequence
 * Signature: (JJ)[J
 */
JNIEXPORT jlongArray JNICALL Java_SlpToolKit_NativeLexique_extractSequence
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    insert
 * Signature: (JLjava/lang/String;[JZDZJZJZJ)J
 */
JNIEXPORT jlong JNICALL Java_SlpToolKit_NativeLexique_insert
  (JNIEnv *, jclass, jlong, jstring, jlongArray, jboolean, jdouble, jboolean, jlong, jboolean, jlong, jboolean, jlong);

/*
 * Class:     SlpToolKit_NativeLexique
 * Method:    internalContains
 * Signature: (JLjava/lang/String;[JZDZJZJZJ)Z
 */
JNIEXPORT jboolean JNICALL Java_SlpToolKit_NativeLexique_internalContains
  (JNIEnv *, jclass, jlong, jstring, jlongArray, jboolean, jdouble, jboolean, jlong, jboolean, jlong, jboolean, jlong);

#ifdef __cplusplus
}
#endif
#endif
