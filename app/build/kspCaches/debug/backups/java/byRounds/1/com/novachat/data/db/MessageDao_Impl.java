package com.novachat.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.novachat.data.db.entities.MessageEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MessageDao_Impl implements MessageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MessageEntity> __insertionAdapterOfMessageEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateContent;

  private final SharedSQLiteStatement __preparedStmtOfFinalizeMessage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByChat;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public MessageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMessageEntity = new EntityInsertionAdapter<MessageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `messages` (`id`,`chatId`,`role`,`content`,`errorText`,`modelId`,`usageIn`,`usageOut`,`stoppedManually`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MessageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getChatId());
        statement.bindString(3, entity.getRole());
        statement.bindString(4, entity.getContent());
        if (entity.getErrorText() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getErrorText());
        }
        if (entity.getModelId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getModelId());
        }
        if (entity.getUsageIn() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getUsageIn());
        }
        if (entity.getUsageOut() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getUsageOut());
        }
        final int _tmp = entity.getStoppedManually() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfUpdateContent = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET content = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfFinalizeMessage = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE messages SET content = ?, usageIn = ?,\n"
                + "           usageOut = ?, errorText = ?,\n"
                + "           stoppedManually = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByChat = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages WHERE chatId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM messages";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final MessageEntity message, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMessageEntity.insert(message);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateContent(final String id, final String content,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateContent.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, content);
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateContent.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object finalizeMessage(final String id, final String content, final Integer usageIn,
      final Integer usageOut, final String errorText, final boolean stoppedManually,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfFinalizeMessage.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, content);
        _argIndex = 2;
        if (usageIn == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, usageIn);
        }
        _argIndex = 3;
        if (usageOut == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, usageOut);
        }
        _argIndex = 4;
        if (errorText == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, errorText);
        }
        _argIndex = 5;
        final int _tmp = stoppedManually ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 6;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfFinalizeMessage.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByChat(final String chatId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByChat.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByChat.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MessageEntity>> observeMessages(final String chatId) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<List<MessageEntity>>() {
      @Override
      @NonNull
      public List<MessageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfErrorText = CursorUtil.getColumnIndexOrThrow(_cursor, "errorText");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfUsageIn = CursorUtil.getColumnIndexOrThrow(_cursor, "usageIn");
          final int _cursorIndexOfUsageOut = CursorUtil.getColumnIndexOrThrow(_cursor, "usageOut");
          final int _cursorIndexOfStoppedManually = CursorUtil.getColumnIndexOrThrow(_cursor, "stoppedManually");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<MessageEntity> _result = new ArrayList<MessageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MessageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpErrorText;
            if (_cursor.isNull(_cursorIndexOfErrorText)) {
              _tmpErrorText = null;
            } else {
              _tmpErrorText = _cursor.getString(_cursorIndexOfErrorText);
            }
            final String _tmpModelId;
            if (_cursor.isNull(_cursorIndexOfModelId)) {
              _tmpModelId = null;
            } else {
              _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            }
            final Integer _tmpUsageIn;
            if (_cursor.isNull(_cursorIndexOfUsageIn)) {
              _tmpUsageIn = null;
            } else {
              _tmpUsageIn = _cursor.getInt(_cursorIndexOfUsageIn);
            }
            final Integer _tmpUsageOut;
            if (_cursor.isNull(_cursorIndexOfUsageOut)) {
              _tmpUsageOut = null;
            } else {
              _tmpUsageOut = _cursor.getInt(_cursorIndexOfUsageOut);
            }
            final boolean _tmpStoppedManually;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfStoppedManually);
            _tmpStoppedManually = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new MessageEntity(_tmpId,_tmpChatId,_tmpRole,_tmpContent,_tmpErrorText,_tmpModelId,_tmpUsageIn,_tmpUsageOut,_tmpStoppedManually,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<MessageEntity> observeLastMessage(final String chatId) {
    final String _sql = "SELECT * FROM messages WHERE chatId = ? ORDER BY createdAt DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"messages"}, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfErrorText = CursorUtil.getColumnIndexOrThrow(_cursor, "errorText");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfUsageIn = CursorUtil.getColumnIndexOrThrow(_cursor, "usageIn");
          final int _cursorIndexOfUsageOut = CursorUtil.getColumnIndexOrThrow(_cursor, "usageOut");
          final int _cursorIndexOfStoppedManually = CursorUtil.getColumnIndexOrThrow(_cursor, "stoppedManually");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpErrorText;
            if (_cursor.isNull(_cursorIndexOfErrorText)) {
              _tmpErrorText = null;
            } else {
              _tmpErrorText = _cursor.getString(_cursorIndexOfErrorText);
            }
            final String _tmpModelId;
            if (_cursor.isNull(_cursorIndexOfModelId)) {
              _tmpModelId = null;
            } else {
              _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            }
            final Integer _tmpUsageIn;
            if (_cursor.isNull(_cursorIndexOfUsageIn)) {
              _tmpUsageIn = null;
            } else {
              _tmpUsageIn = _cursor.getInt(_cursorIndexOfUsageIn);
            }
            final Integer _tmpUsageOut;
            if (_cursor.isNull(_cursorIndexOfUsageOut)) {
              _tmpUsageOut = null;
            } else {
              _tmpUsageOut = _cursor.getInt(_cursorIndexOfUsageOut);
            }
            final boolean _tmpStoppedManually;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfStoppedManually);
            _tmpStoppedManually = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpRole,_tmpContent,_tmpErrorText,_tmpModelId,_tmpUsageIn,_tmpUsageOut,_tmpStoppedManually,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getById(final String id, final Continuation<? super MessageEntity> $completion) {
    final String _sql = "SELECT * FROM messages WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MessageEntity>() {
      @Override
      @Nullable
      public MessageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChatId = CursorUtil.getColumnIndexOrThrow(_cursor, "chatId");
          final int _cursorIndexOfRole = CursorUtil.getColumnIndexOrThrow(_cursor, "role");
          final int _cursorIndexOfContent = CursorUtil.getColumnIndexOrThrow(_cursor, "content");
          final int _cursorIndexOfErrorText = CursorUtil.getColumnIndexOrThrow(_cursor, "errorText");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfUsageIn = CursorUtil.getColumnIndexOrThrow(_cursor, "usageIn");
          final int _cursorIndexOfUsageOut = CursorUtil.getColumnIndexOrThrow(_cursor, "usageOut");
          final int _cursorIndexOfStoppedManually = CursorUtil.getColumnIndexOrThrow(_cursor, "stoppedManually");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final MessageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpChatId;
            _tmpChatId = _cursor.getString(_cursorIndexOfChatId);
            final String _tmpRole;
            _tmpRole = _cursor.getString(_cursorIndexOfRole);
            final String _tmpContent;
            _tmpContent = _cursor.getString(_cursorIndexOfContent);
            final String _tmpErrorText;
            if (_cursor.isNull(_cursorIndexOfErrorText)) {
              _tmpErrorText = null;
            } else {
              _tmpErrorText = _cursor.getString(_cursorIndexOfErrorText);
            }
            final String _tmpModelId;
            if (_cursor.isNull(_cursorIndexOfModelId)) {
              _tmpModelId = null;
            } else {
              _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            }
            final Integer _tmpUsageIn;
            if (_cursor.isNull(_cursorIndexOfUsageIn)) {
              _tmpUsageIn = null;
            } else {
              _tmpUsageIn = _cursor.getInt(_cursorIndexOfUsageIn);
            }
            final Integer _tmpUsageOut;
            if (_cursor.isNull(_cursorIndexOfUsageOut)) {
              _tmpUsageOut = null;
            } else {
              _tmpUsageOut = _cursor.getInt(_cursorIndexOfUsageOut);
            }
            final boolean _tmpStoppedManually;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfStoppedManually);
            _tmpStoppedManually = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new MessageEntity(_tmpId,_tmpChatId,_tmpRole,_tmpContent,_tmpErrorText,_tmpModelId,_tmpUsageIn,_tmpUsageOut,_tmpStoppedManually,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM messages";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
