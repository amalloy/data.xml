;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc ""
  :author "Chris Houser"}
  clojure.data.xml.pull-parser
  (:use [clojure.data.xml :as xml :only []])
  (:import (javax.xml.stream XMLInputFactory XMLStreamReader
            XMLStreamConstants)
           (java.io Reader)))

(defn- attr-hash [^XMLStreamReader sreader] (into {}
    (for [i (range (.getAttributeCount sreader))]
      [(keyword (.getAttributePrefix sreader i) (.getAttributeLocalName sreader i))
       (.getAttributeValue sreader i)])))

; Note, sreader is mutable and mutated here in pull-seq, but it's
; protected by a lazy-seq so it's thread-safe.
(defn- pull-seq [^XMLStreamReader sreader]
  (lazy-seq
    (loop []
      (condp == (.next sreader)
        XMLStreamConstants/START_ELEMENT
          (cons (xml/event :start-element
                           (keyword (.getLocalName sreader))
                           (attr-hash sreader) nil)
                  (pull-seq sreader)) 
        XMLStreamConstants/END_ELEMENT
          (cons (xml/event :end-element
                           (keyword (.getLocalName sreader)) nil nil)
                (pull-seq sreader))
        XMLStreamConstants/CHARACTERS
          (let [text (.getText sreader)]
            (cons (xml/event :characters nil nil text)
                  (pull-seq sreader)))
        XMLStreamConstants/END_DOCUMENT
          nil))))

(defn lazy-source-seq
  "Parses the XML InputSource source using a pull-parser. Returns
  a lazy sequence of Event records.  See clojure.data.xml/lazy-source-seq
  for similar results but without requiring an external pull parser."
  [^java.io.InputStream s]
  (let [sreader (.createXMLStreamReader (XMLInputFactory/newInstance) s)]
    ;(.setNamespaceAttributesReporting xpp true)
    ;(.setInput xpp s)
    (pull-seq sreader)))

(defn lazy-parse
  "Convenience function. Parses the source, which can be a File,
  InputStream or String naming a URI, and returns a lazy tree of
  Element records. See lazy-source-seq for finer-grained control."
  [source]
  (xml/event-tree (lazy-source-seq source)))
