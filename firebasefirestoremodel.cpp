#include "firebasefirestoremodel.h"
#include <android/log.h>



FirebaseFirestoreModel* FirebaseFirestoreModel::instance_;
const QString FirebaseFirestoreModel::kJavaClassName = "com/qt/plugin/firebase/QtFirebaseFirestore";

FirebaseFirestoreModel::FirebaseFirestoreModel(QObject *parent)
    : QObject(parent)
{
    // By default, QQuickItem does not draw anything. If you subclass
    // QQuickItem to create a visual item, you will need to uncomment the
    // following line and re-implement updatePaintNode()

    // setFlag(ItemHasContents, true);
    FirebaseFirestoreModel::instance_ = this;

    qDebug() << kTag << "init";

    QJniObject obj(FirebaseFirestoreModel::kJavaClassName.toLocal8Bit().constData());
     qDebug() << kTag << "pre java register";
    obj.callMethod<void>("register");
    qDebug() << kTag << "java registered";



    QtMobilePluginDispatcher* channel = QtMobilePluginDispatcher::instance();
    QObject::connect(channel, &QtMobilePluginDispatcher::messageReceived,
            this, &FirebaseFirestoreModel::setMessage);
    qDebug() << kTag << "channel event  created";


}

FirebaseFirestoreModel::~FirebaseFirestoreModel()
{

}

void FirebaseFirestoreModel::setMessage(QtMobilePluginChannelMessage* message){

    qDebug() << kTag << "setMessage received: " << message->getMethodName();

    if(message->hasError()){
        emit FirebaseFirestoreModel::error(message->getId(), message->getErrorDescription());
    }else{

        if(message->getChannelName() != this->kChannelName){
            qDebug() << kTag << "descart channel " << message->getChannelName() << " message ";
            return;
        }

        qDebug() << kTag << "message dump: " << message->dump();

        if(message->getMethodName() == this->kMethodGetDocuments){

            QString collection = message->getData(0).toString();
            JavaUtilList list = message->getData(1);
            JavaUtilIterator it = list.iterator();
            QVariantList results;

            while(it.hasNext()){
                JavaUtilMap map = it.next();
                QList<JavaUtilMapEntry> mapEntry = map.entrySet();
                QVariantMap data;
                for(JavaUtilMapEntry entry : mapEntry){
                    QString key = entry.getKey().toString();
                    QString value = entry.getValue().toString();
                    data[key] = value;
                }
                results << data;
            }

            emit FirebaseFirestoreModel::collection(collection, results);
        }else{
            qDebug() << kTag << "message method " << message->getMethodName() << " not handled";
        }
    }
}

QString FirebaseFirestoreModel::getDocuments(QString const &docName){
    qDebug() << kTag << "getDocuments";
    QtMobilePluginDispatcher* channel = QtMobilePluginDispatcher::instance();
    QtMobilePluginChannelMessage* message = QtMobilePluginChannelMessage::newJavaObject(this->kChannelName, this->kMethodGetDocuments);
    message->withArg(docName);
    channel->dispatch(message);
    return message->getId();
}

QString FirebaseFirestoreModel::init(){
    qDebug() << kTag << "init";
    QtMobilePluginDispatcher * channel = QtMobilePluginDispatcher::instance();
    QtMobilePluginChannelMessage* message = QtMobilePluginChannelMessage::newJavaObject(this->kChannelName, this->kMethodInit);
    channel->dispatch(message);
    return message->getId();
}

QString FirebaseFirestoreModel::removeDocument(const QString &docName, const QString &docId){
    qDebug() << kTag << "removeDocument";
    QtMobilePluginDispatcher* channel = QtMobilePluginDispatcher::instance();
    QtMobilePluginChannelMessage* message = QtMobilePluginChannelMessage::newJavaObject(this->kChannelName, this->kMethodRemoveDocument);
    message->withArg(docName)->withArg(docId);
    channel->dispatch(message);
    return message->getId();
}

QString FirebaseFirestoreModel::addDocument(const QString &docName, const QVariantMap &data){
    qDebug() << kTag << "addDocument";

    JavaUtilMap map = JavaUtilMap::newObject();

    for(auto&& key : data.keys()){

        auto val = data[key];

        if(val.typeId() == QMetaType::QString){
            map.put(key, val.toString());
        }else if(val.typeId() == QMetaType::Int){
            map.put(key, val.toInt());
        }else if(val.typeId() == QMetaType::Double){
            map.put(key, val.toDouble());
        }else if(val.typeId() == QMetaType::Bool){
            map.put(key, val.toBool());
        }
        /*else if(val.typeId() == QMetaType::Date){
            //map.put(key, val.toDate().toString("yyyy-MM-dd"));
        } else if(val.typeId() == QMetaType::type()){

        }*/

    }

    QtMobilePluginDispatcher* channel = QtMobilePluginDispatcher::instance();
    QtMobilePluginChannelMessage* message = QtMobilePluginChannelMessage::newJavaObject(this->kChannelName, this->kMethodRemoveDocument);
    message->withArg(docName)->withArg(map);
    channel->dispatch(message);
    return message->getId();
}
