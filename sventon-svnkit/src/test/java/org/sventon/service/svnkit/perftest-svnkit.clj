(ns org.sventon.service.svnkit.perftest-svnkit
  (:import [org.sventon.service RepositoryService]
    [org.sventon SVNConnection]
    [org.sventon.service.svnkit SVNKitConnection SVNKitRepositoryService]
    [org.sventon.export ExportDirectory]
    [org.sventon.model PathRevision Revision]
    [org.sventon.web.command DiffCommand]
    [org.tmatesoft.svn.core SVNURL]
    [org.tmatesoft.svn.core.internal.io.dav DAVRepositoryFactory]
    [org.tmatesoft.svn.core.internal.io.fs FSRepositoryFactory]
    [org.tmatesoft.svn.core.internal.io.svn SVNRepositoryFactoryImpl]
    [org.tmatesoft.svn.core.io SVNRepository SVNRepositoryFactory]
    [java.io OutputStream]
    [org.mockito Mockito])
  )

(defn create-connection [url]
  (do (SVNRepositoryFactoryImpl/setup)
    (DAVRepositoryFactory/setup)
    (FSRepositoryFactory/setup)
    (SVNKitConnection. (SVNRepositoryFactory/create (SVNURL/parseURIDecoded url)))))

(defn create-service []
  (SVNKitRepositoryService.))

(defn get-latest-revision [url]
  (let [connection (create-connection url)
        service (create-service)]
    (.getLatestRevision service connection)))

(defn get-log-entry [url rev]
  (let [connection (create-connection url)
        service (create-service)]
    (.getLogEntry service nil connection rev)))

(defn get-log-entries-from-repository-root [url from-rev to-rev]
  (let [connection (create-connection url)
        service (create-service)]
    (.getLogEntriesFromRepositoryRoot service connection from-rev to-rev)))

(defn get-log-entries
  [url from-rev to-rev path limit stop-on-copy? include-changed-paths?]
  (let [connection (create-connection url)
        service (create-service)]
    (.getLogEntries service nil connection from-rev to-rev path limit stop-on-copy? include-changed-paths?)))

(defn export [url targets peg-revision export-dir]
  (let [connection (create-connection url)
        service (create-service)]
    (.export service connection targets peg-revision export-dir)))

(defn get-file-contents [url path revision output]
  (let [connection (create-connection url)
        service (create-service)]
    (.getFileContents service connection path revision output)))

(defn list-properties [url path revision]
  (let [connection (create-connection url)
        service (create-service)]
    (.listProperties service connection path revision)))

(defn get-node-kind [url path revision]
  (let [connection (create-connection url)
        service (create-service)]
    (.getNodeKind service connection path revision)))

(defn get-locks [url start-path recursive?]
  (let [connection (create-connection url)
        service (create-service)]
    (.getLocks service connection start-path recursive?)))

(defn list-dir [url path revision]
  (let [connection (create-connection url)
        service (create-service)]
    (.list service connection path revision)))

(defn get-entry-info [url path revision]
  (let [connection (create-connection url)
        service (create-service)]
    (.getEntryInfo service connection path revision)))

(defn get-file-revisions [url path revision]
  (let [connection (create-connection url)
        service (create-service)]
    (.getFileRevisions service connection path revision)))

(defn diff-sideby-side [url diff-command peg-rev charset]
  (let [connection (create-connection url)
        service (create-service)]
    (.diffSideBySide service connection diff-command peg-rev charset)))

(defn diff-unified [url diff-command peg-rev charset]
  (let [connection (create-connection url)
        service (create-service)]
    (.diffUnified service connection diff-command peg-rev charset)))

(defn diff-inline [url diff-command peg-rev charset]
  (let [connection (create-connection url)
        service (create-service)]
    (.diffInline service connection diff-command peg-rev charset)))

(defn diff-paths [url diff-command]
  (let [connection (create-connection url)
        service (create-service)]
    (.diffPaths service connection diff-command)))


(defn time-method [n m]
  (time (dotimes [_ n] (m))))


(def root "svn://localhost/sventon-repo-dump")

(defn run-tests [n]
  (do
    (println "Running tests agains:" root "with" n "samples")

    (println "getLatestRevision")
    (time-method n #(get-latest-revision root))

    (println "getLogEntry")
    (time-method n #(get-log-entry root 1800))

    (println "getLogEntriesFromRepositoryRoot")
    (time-method n #(get-log-entries-from-repository-root root 1000 1800))

    (println "getLogEntries")
    (time-method n #(get-log-entries root 1000 1800 "/trunk/sventon/src/main/java/org/sventon", 1000, false true))

    (comment
      (println "export")
      (let [paths [(PathRevision. "/trunk/lib" (Revision/parse "1800")),
                   (PathRevision. "/trunk/licenses" (Revision/parse "1800")),
                   (PathRevision. "/trunk/sventon" (Revision/parse "1800"))]]
        (time-method n #(export root paths 1800 (Mockito/mock ExportDirectory))))
      )

    (println "getFileContents")
    (time-method n
      #(get-file-contents
        root
        "/trunk/sventon/src/main/java/org/sventon/AuthenticationException.java"
        1800
        (Mockito/mock OutputStream)))

    (println "listProperties")
    (time-method n
      #(list-properties
        root
        "/trunk/sventon/src/main/java/org/sventon/AuthenticationException.java"
        1800))

    (println "getNodeKind")
    (time-method n
      #(get-node-kind
        root
        "/trunk/sventon/src/main/java/org/sventon/AuthenticationException.java"
        1800))

    (println "getLocks")
    (time-method n
      #(get-locks root "/trunk" true))

    (println "list")
    (time-method n
      #(list-dir root "/trunk" 1800))

    (println "getEntryInfo")
    (time-method n
      #(get-entry-info
        root
        "/trunk/sventon/src/main/java/org/sventon/AuthenticationException.java"
        1800))

    (println "getFileRevisions")
    (time-method n
      #(get-file-revisions
        root
        "/trunk/sventon/src/main/java/org/sventon/AuthenticationException.java"
        1800))


    (let [command
          (doto (DiffCommand.)
            (.setEntries (into-array [
              (PathRevision.
                "/trunk/sventon-core/src/main/java/org/sventon/AuthenticationException.java"
                (Revision/parse "1791")),
              (PathRevision.
                "/branches/features/svn_facade/sventon/src/main/java/org/sventon/SVNAuthenticationException.java"
                (Revision/parse "1698"))])))]

      (println "diffSideBySide")
      (time-method n
        #(diff-sideby-side root command (Revision/parse "1800") "UTF-8"))

      (println "diffUnified")
      (time-method n
        #(diff-unified root command (Revision/parse "1800") "UTF-8"))

      (println "diffInline")
      (time-method n
        #(diff-sideby-side root command (Revision/parse "1800") "UTF-8"))

      (println "diffPaths")
      (time-method n
        #(diff-paths root command)))))

;(run-tests 1)

