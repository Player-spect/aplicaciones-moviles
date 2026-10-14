package com.example.evaluacion_01

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "iot_app_local.db"
        const val DATABASE_VERSION = 3

        const val TABLE_USERS = "users_local"

        const val COLUMN_LOCAL_ID = "local_id"
        const val COLUMN_ID = "id" // ID remoto de AWS
        const val COLUMN_USERNAME = "username"
        const val COLUMN_NOMBRES = "nombres"
        const val COLUMN_APELLIDOS = "apellidos"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD_HASH = "password_hash"
        const val COLUMN_IS_ACTIVE = "is_active"
        const val COLUMN_RECOVERY_CODE = "recovery_code"
        const val COLUMN_RECOVERY_EXPIRES_AT = "recovery_expires_at"
        const val COLUMN_CREATED_AT = "created_at"

        // 0: Sincronizado, 1: Pendiente Insert, 2: Pendiente Update, 3: Pendiente Delete
        const val COLUMN_SYNC_STATUS = "sync_status"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_LOCAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID INTEGER ,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_NOMBRES TEXT NOT NULL,
                $COLUMN_APELLIDOS TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PASSWORD_HASH TEXT NOT NULL,
                $COLUMN_IS_ACTIVE INTEGER NOT NULL DEFAULT 1,
                $COLUMN_RECOVERY_CODE TEXT,
                $COLUMN_RECOVERY_EXPIRES_AT TEXT,
                $COLUMN_CREATED_AT TEXT NOT NULL,
                $COLUMN_SYNC_STATUS INTEGER NOT NULL DEFAULT 0
            )
        """
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun getAllLocalUsers(): List<ContentValues> {
        val list = mutableListOf<ContentValues>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_SYNC_STATUS != 3", null)

        if (cursor.moveToFirst()) {
            do {
                val cv = ContentValues().apply {
                    put(COLUMN_LOCAL_ID, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_ID)))
                    put(COLUMN_ID, if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ID))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                    put(COLUMN_USERNAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)))
                    put(COLUMN_NOMBRES, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRES)))
                    put(COLUMN_APELLIDOS, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APELLIDOS)))
                    put(COLUMN_EMAIL, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)))
                    put(COLUMN_PASSWORD_HASH, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)))
                    put(COLUMN_IS_ACTIVE, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)))
                    put(COLUMN_RECOVERY_CODE, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECOVERY_CODE)))
                    put(COLUMN_RECOVERY_EXPIRES_AT, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_RECOVERY_EXPIRES_AT)))
                    put(COLUMN_CREATED_AT, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)))
                    put(COLUMN_SYNC_STATUS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)))
                }
                list.add(cv)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun saveOrUpdateUserFromAws(remoteId: Int, username: String, nombres: String, apellidos: String, email: String, passwordHash: String, createdAt: String) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_LOCAL_ID, $COLUMN_SYNC_STATUS FROM $TABLE_USERS WHERE $COLUMN_ID = ? OR $COLUMN_EMAIL = ? ORDER BY $COLUMN_SYNC_STATUS ASC", arrayOf(remoteId.toString(), email))

        if (cursor.moveToFirst()) {
            val localId = cursor.getInt(0)
            val syncStatus = cursor.getInt(1)
            
            val values = ContentValues().apply {
                put(COLUMN_ID, remoteId)
                if (syncStatus == 0) {
                    put(COLUMN_NOMBRES, nombres)
                    put(COLUMN_APELLIDOS, apellidos)
                    put(COLUMN_EMAIL, email)
                    put(COLUMN_SYNC_STATUS, 0)
                } else if (syncStatus == 1) {
                    // Si estaba pendiente de insertar pero ya viene de AWS,
                    // asumimos que el insert ya se reflejó o se reemplaza por el remoto
                    put(COLUMN_NOMBRES, nombres)
                    put(COLUMN_APELLIDOS, apellidos)
                    put(COLUMN_EMAIL, email)
                    put(COLUMN_SYNC_STATUS, 0)
                }
                // Si es 2 (Pendiente Update) o 3 (Pendiente Delete), no tocamos los datos ni el syncStatus,
                // solo le actualizamos el COLUMN_ID para que la próxima sincronización use el ID correcto.
            }
            db.update(TABLE_USERS, values, "$COLUMN_LOCAL_ID = ?", arrayOf(localId.toString()))

            // Eliminar duplicados adicionales si existieran con el mismo email
            if (cursor.moveToNext()) {
                do {
                    val duplicateLocalId = cursor.getInt(0)
                    db.delete(TABLE_USERS, "$COLUMN_LOCAL_ID = ?", arrayOf(duplicateLocalId.toString()))
                } while (cursor.moveToNext())
            }
        } else {
            val values = ContentValues().apply {
                put(COLUMN_ID, remoteId)
                put(COLUMN_USERNAME, username)
                put(COLUMN_NOMBRES, nombres)
                put(COLUMN_APELLIDOS, apellidos)
                put(COLUMN_EMAIL, email)
                put(COLUMN_PASSWORD_HASH, passwordHash)
                put(COLUMN_IS_ACTIVE, 1)
                put(COLUMN_CREATED_AT, createdAt)
                put(COLUMN_SYNC_STATUS, 0)
            }
            db.insert(TABLE_USERS, null, values)
        }
        cursor.close()
        db.close()
    }

    fun insertUserOffline(
        username: String,
        nombres: String,
        apellidos: String,
        email: String,
        passwordHash: String,
        createdAt: String
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_NOMBRES, nombres)
            put(COLUMN_APELLIDOS, apellidos)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD_HASH, passwordHash)
            put(COLUMN_IS_ACTIVE, 1)
            put(COLUMN_CREATED_AT, createdAt)
            put(COLUMN_SYNC_STATUS, 1) // 1 = Pendiente de Insertar
        }
        val id = db.insert(TABLE_USERS, null, values)
        db.close()
        return id
    }

    // --- ACTUALIZAR USUARIO OFFLINE ---
    fun updateUserOffline(remoteIdOrLocalId: Int, nombres: String, apellidos: String, email: String, isRemote: Boolean = true): Int {
        val db = this.writableDatabase
        // Buscamos tanto por COLUMN_ID como por COLUMN_LOCAL_ID para asegurar que encuentre al usuario (sea remoto o creado offline)
        val selectionClause = "$COLUMN_ID = ? OR $COLUMN_LOCAL_ID = ?"

        // Consultamos el estado actual
        val cursor = db.rawQuery("SELECT $COLUMN_SYNC_STATUS, $COLUMN_LOCAL_ID FROM $TABLE_USERS WHERE $selectionClause", arrayOf(remoteIdOrLocalId.toString(), remoteIdOrLocalId.toString()))
        var rows = 0

        if (cursor.moveToFirst()) {
            val currentStatus = cursor.getInt(0)
            val localId = cursor.getInt(1)

            // Si era estado 1 (nuevo offline), se queda en 1. Si ya estaba sincronizado (0), pasa a 2 (pendiente update).
            val newStatus = if (currentStatus == 1) 1 else 2

            val values = ContentValues().apply {
                put(COLUMN_NOMBRES, nombres)
                put(COLUMN_APELLIDOS, apellidos)
                put(COLUMN_EMAIL, email)
                put(COLUMN_SYNC_STATUS, newStatus)
            }
            rows = db.update(TABLE_USERS, values, "$COLUMN_LOCAL_ID = ?", arrayOf(localId.toString()))
        }
        cursor.close()
        db.close()
        return rows
    }

    fun getPendingSyncUsers(): List<ContentValues> {
        val list = mutableListOf<ContentValues>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USERS WHERE $COLUMN_SYNC_STATUS != 0", null)

        if (cursor.moveToFirst()) {
            do {
                val cv = ContentValues().apply {
                    put(COLUMN_LOCAL_ID, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_ID)))
                    put(COLUMN_ID, if (cursor.isNull(cursor.getColumnIndexOrThrow(COLUMN_ID))) null else cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                    put(COLUMN_USERNAME, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)))
                    put(COLUMN_NOMBRES, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOMBRES)))
                    put(COLUMN_APELLIDOS, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_APELLIDOS)))
                    put(COLUMN_EMAIL, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)))
                    put(COLUMN_PASSWORD_HASH, cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD_HASH)))
                    put(COLUMN_IS_ACTIVE, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ACTIVE)))
                    put(COLUMN_SYNC_STATUS, cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)))
                }
                list.add(cv)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun updateSyncStatusAfterSync(localId: Int, remoteId: Int, newStatus: Int = 0) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, remoteId)
            put(COLUMN_SYNC_STATUS, newStatus)
        }
        db.update(TABLE_USERS, values, "$COLUMN_LOCAL_ID = ?", arrayOf(localId.toString()))
        db.close()
    }

    fun cleanUpDeletedUsers(remoteIds: List<Int>) {
        val db = this.writableDatabase
        if (remoteIds.isEmpty()) {
            // Si la lista de AWS está vacía, eliminamos todos los usuarios sincronizados (0)
            db.delete(TABLE_USERS, "$COLUMN_SYNC_STATUS = 0", null)
        } else {
            val placeholders = remoteIds.joinToString(",") { "?" }
            val args = remoteIds.map { it.toString() }.toTypedArray()
            
            // Eliminamos los usuarios que ya están sincronizados (0) pero no están en AWS
            db.delete(TABLE_USERS, "$COLUMN_SYNC_STATUS = 0 AND $COLUMN_ID NOT IN ($placeholders)", args)
        }
        db.close()
    }

    fun markUserAsDeletedOffline(localId: Int) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_SYNC_STATUS, $COLUMN_ID FROM $TABLE_USERS WHERE $COLUMN_LOCAL_ID = $localId", null)

        if (cursor.moveToFirst()) {
            val syncStatus = cursor.getInt(0)
            val remoteIdIsNull = cursor.isNull(1)

            if (syncStatus == 1 && remoteIdIsNull) {
                db.delete(TABLE_USERS, "$COLUMN_LOCAL_ID = ?", arrayOf(localId.toString()))
            } else {
                val values = ContentValues().apply {
                    put(COLUMN_SYNC_STATUS, 3) // 3 = Pendiente de Eliminar en AWS
                }
                db.update(TABLE_USERS, values, "$COLUMN_LOCAL_ID = ?", arrayOf(localId.toString()))
            }
        }
        cursor.close()
        db.close()
    }
}