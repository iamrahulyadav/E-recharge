package com.bdlions.sampanit.database;

/**
 * Created by sampanit on 22/06/16.
 */
public class DBQuery {
 public static String create_user =  "create table " + QueryField.TABLE_USERS +" (id INTEGER PRIMARY KEY AUTOINCREMENT,user_id INTEGER, session_id TEXT, username TEXT, base_url TEXT, opcode TEXT, password TEXT, pin_code TEXT)" ;
}
