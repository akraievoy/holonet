package org.akraievoy.db;

import org.akraievoy.couch.CouchDao;
import org.akraievoy.couch.Squab;

public class CouchDb {

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
		couch.findAllPaths(Squab.class, "");  // FIXME Empty path parameter
	}

}
