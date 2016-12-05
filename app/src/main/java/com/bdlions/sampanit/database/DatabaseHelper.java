package com.bdlions.sampanit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by sampanit on 14/06/16.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper sInstance = null;
    private Context context;

    public static DatabaseHelper getInstance(Context ctx) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return sInstance;
    }
    private DatabaseHelper(Context context) {
        super(context, QueryField.DATABASE_NAME, null, QueryField.DATABASE_VERSION);
        this.context = context;
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBQuery.create_user);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {


    }
    public boolean createUser(int userId, String userName, String password, String opcode, String baseUrl, String sessionId){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(QueryField.USER_ID,userId);
        contentValues.put(QueryField.SESSION_ID, sessionId);
        contentValues.put(QueryField.USER_NMAE,userName);
        contentValues.put(QueryField.BASE_URL,baseUrl);
        contentValues.put(QueryField.OP_CODE,opcode);
        contentValues.put(QueryField.PASSWORD,password);
        long result = db.insert(QueryField.TABLE_USERS, null, contentValues);
        db.close();
        if(result == -1)
            return false;
        else
            return true;
    }

    public JSONObject getUserInfo() {
        int localUserId = 0;
        String selectQuery = "SELECT  * FROM " + QueryField.TABLE_USERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
      /*  if(cursor.moveToFirst()){
            while (cursor.moveToFirst()){

            }
        }*/
        JSONObject userInfo = new JSONObject();
        if (cursor.moveToFirst()) {
            do {
                try {
                    userInfo.put("userId", cursor.getInt(cursor.getColumnIndex(QueryField.USER_ID)));
                    userInfo.put("sessionId", cursor.getString(cursor.getColumnIndex(QueryField.SESSION_ID)));
                    userInfo.put("userName", cursor.getString(cursor.getColumnIndex(QueryField.USER_NMAE)));
                    userInfo.put("baseUrl", cursor.getString(cursor.getColumnIndex(QueryField.BASE_URL)));
                    userInfo.put("opCode", cursor.getString(cursor.getColumnIndex(QueryField.OP_CODE)));
                    userInfo.put("password", cursor.getString(cursor.getColumnIndex(QueryField.PASSWORD)));
                    userInfo.put("pinCode", cursor.getString(cursor.getColumnIndex(QueryField.PIN_CODE)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return userInfo;
    }

    public boolean checkLogin(){

        // Check login status
        String selectQuery = "SELECT  * FROM " + QueryField.TABLE_USERS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor mCursor = db.rawQuery(selectQuery, null);

        if (!(mCursor.moveToFirst()) || mCursor.getCount() ==0){
            return false;
            //cursor is empty
        }
        mCursor.close();

        return true;
    }
    public int updatePinCode(int userId, String pinCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues updateUserInfo = new ContentValues();
        updateUserInfo.put(QueryField.PIN_CODE, pinCode);
        // updating row
        return db.update(QueryField.TABLE_USERS, updateUserInfo, QueryField.USER_ID + " = ?",
                new String[] { String.valueOf(userId) });
    }

    public void deleteUserInfo(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(QueryField.TABLE_USERS, QueryField.USER_ID + " = ?",
                new String[]{String.valueOf(userId)});
        db.close();
    }


   /* public void addServiceList(){
        String insertQuery = "INSERT INTO mytable (col1, col2, col3) VALUES (1, 2, 'abc'),(2, 4, 'xyz')";
        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL(insertQuery);

    }*/






}
