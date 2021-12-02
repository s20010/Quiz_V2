package jp.ac.it_college.std.s20010.quizv2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBaseHelper (context: Context, ) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "eitodatabese.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCode = """
            create table eito(
            _id integer primary key,
            question text,
            answers integer,
            choices text
            );
        """.trimIndent()
        db.execSQL(createCode)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        println("アップグレード")
    }
}