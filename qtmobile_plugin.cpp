//#include "qtmobile_plugin.h"

//#include <firebasefirestoremodel.h>

//#include <qqml.h>

//void QtMobilePlugin::registerTypes(const char *uri)
//{
//    // @uri br.com.mobilemind.qt.firebase
//    qmlRegisterType<FirebaseFirestoreModel>(uri, 1, 0, "QtMobilePluginFirebase");
//}


#ifndef QTMOBILEPLUGIN_PLUGIN_H
#define QTMOBILEPLUGIN_PLUGIN_H

#include <QtQml/QQmlExtensionPlugin>

class QtMobilePlugin : public QQmlEngineExtensionPlugin
{
    Q_OBJECT
    Q_PLUGIN_METADATA(IID QQmlEngineExtensionInterface_iid)

//public:
//void registerTypes(const char *uri) override;
};

#include "qtmobile_plugin.moc"

#endif // QTMOBILEPLUGIN_PLUGIN_H
