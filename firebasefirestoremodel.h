#ifndef FIREBASE_H
#define FIREBASE_H

#include <QtQml>
#include <QJniObject>
#include <QJniEnvironment>
#include <QList>
#include "qtmobilefirebaseglobal.h"

//![0]
class FirebaseFirestoreModel : public QObject
{
    Q_OBJECT
    //Q_DISABLE_COPY(Firebase)
     QML_NAMED_ELEMENT(FirebaseFirestore)
//![0]
private:
    void registerNativeMethods();
    static FirebaseFirestoreModel *instance_;
    const QString kChannelName = "com.qt.plugin.firebase.Firestore";
    const QString kMethodGetDocuments = "getDocuments";
    const QString kMethodUpdateDocument = "updateDocument";
    const QString kMethodAddDocument = "addDocument";
    const QString kMethodRemoveDocument = "removeDocument";
    const QString kMethodAddOrUpdateDocumentMulti = "addOrUpdateDocumentMulti";
    const QString kMethodAddCollectionListener = "addCollectionListener";
    const QString kMethodRemoveCollectionListener = "removeCollectionListener";
    const QString kMethodInit = "init";
    const QString kTag = "FirebaseFirestoreModel: ";

public:
    explicit FirebaseFirestoreModel(QObject *parent = nullptr);
    static FirebaseFirestoreModel  *instance() { return FirebaseFirestoreModel::instance_; }

    ~FirebaseFirestoreModel() override;

    Q_INVOKABLE QString getDocuments(QString const &docName);
    Q_INVOKABLE QString removeDocument(QString const &docName, QString const &docId);
    Q_INVOKABLE QString addDocument(QString const &docName, QVariantMap const &data);
    Q_INVOKABLE QString init();

    static const QString kJavaClassName;


signals:

    void error(QString const &messageId, QString const &error);
    void collection(QString const &collection, QVariantList const &values);
    void success(QString const &messageId);

    //void firestoreResultsFor(QString const &collection, QList<QString> const &values);
    //void firestoreResultsFor2(QString const &collection, QVariantList const &values);
    //void firestoreError(QString const &error);


public slots:
    void setMessage(QtMobilePluginChannelMessage* message);

};

#endif // FIREBASE_H
