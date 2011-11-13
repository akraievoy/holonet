package org.akraievoy.db;

import org.akraievoy.couch.CouchDao;
import org.akraievoy.couch.Squab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CouchDb {
	private static final Logger log = LoggerFactory.getLogger(CouchDb.class);

	protected CouchDao couch;

	public CouchDb(CouchDao couch) {
		this.couch = couch;
	}

	public void start() {
		checkConnection();
	}

	public CouchDao getCouch() {
		return couch;
	}

	public void setCouch(CouchDao couch) {
		this.couch = couch;
	}

	private void checkConnection() {
		try {
			couch.findAllPaths(Squab.class, ""); // FIXME Empty path parameter
		} catch (Exception e) {
			log.error("Connection to couchDB failed.");
		}
	}

}
