TEMPLATE = lib
TARGET = QtMobilePluginFirebase
QT += core qml quick
CONFIG += plugin c++17 qmltypes

QML_IMPORT_NAME = QtMobilePluginFirebase
QML_IMPORT_MAJOR_VERSION = 1.0

DESTDIR = imports/$$QML_IMPORT_NAME

TARGET = $$qtLibraryTarget($$TARGET)
uri = com.qt.plugin

# Input
SOURCES += \
        firebasefirestoremodel.cpp \
        qtmobile_plugin.cpp

HEADERS += \
        firebasefirestoremodel.h \
        qtmobilefirebaseglobal.h

PLUGINFILES += \
    imports/$$QML_IMPORT_NAME/plugin.qml \
    imports/$$QML_IMPORT_NAME/qmldir

android {
    ANDROID_PACKAGE_SOURCE_DIR = $$PWD/android
    DISTFILES += \
        android/build.gradle
}

QT_MOBILE_PLUGIN_PATH = /home/ricardo/sources/qt/QtMobilePlugin/build
#QT_MOBILE_PLUGIN_HEADERS_PATH = $$QT_MOBILE_PLUGIN_PATH/opt/Qt/5.15.2/android/include

#INCLUDEPATH = $$QT_MOBILE_PLUGIN_HEADERS/
if
LIBS += -L$$QT_MOBILE_PLUGIN_PATH -lQtMobilePlugin_$$QT_ARCH

headers.path += $$[QT_INSTALL_HEADERS]/QtMobilePluginFirebase
headers.files += $$PWD/*.h

target.path = $$[QT_INSTALL_QML]/$$QML_IMPORT_NAME

pluginfiles_copy.files = $$PLUGINFILES
pluginfiles_copy.path = $$DESTDIR

pluginfiles_install.files = $$PLUGINFILES $$OUT_PWD/$$DESTDIR/plugins.qmltypes
pluginfiles_install.path = $$[QT_INSTALL_QML]/$$QML_IMPORT_NAME


INSTALLS += target headers pluginfiles_install
COPIES += pluginfiles_copy

OTHER_FILES += $$PLUGINFILES


CONFIG += install_ok  # Do not cargo-cult this!
