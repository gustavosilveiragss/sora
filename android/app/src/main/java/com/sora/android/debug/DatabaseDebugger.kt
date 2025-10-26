package com.sora.android.debug

import android.util.Log
import com.sora.android.data.local.SoraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseDebugger @Inject constructor(
    private val database: SoraDatabase
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun logDatabaseContents() {
        scope.launch {
            try {
                val userCount = database.userDao().getAllUsers().size
                val postCount = database.postDao().getRecentPosts(1000).size
                val countryCount = database.countryDao().getAllCountries().size

                Log.d("DatabaseDebugger", "=== DATABASE CONTENTS ===")
                Log.d("DatabaseDebugger", "Users: $userCount")
                Log.d("DatabaseDebugger", "Posts: $postCount")
                Log.d("DatabaseDebugger", "Countries: $countryCount")
                Log.d("DatabaseDebugger", "========================")

                if (userCount > 0) {
                    val users = database.userDao().getAllUsers().take(3)
                    users.forEach { user ->
                        Log.d("DatabaseDebugger", "User: ${user.username} (ID: ${user.id})")
                    }
                }

                if (postCount > 0) {
                    val posts = database.postDao().getRecentPosts(3)
                    posts.forEach { post ->
                        Log.d("DatabaseDebugger", "Post: ${post.caption?.take(30) ?: "No caption"} by ${post.authorUsername}")
                    }
                }

                if (countryCount > 0) {
                    val countries = database.countryDao().getAllCountries().take(3)
                    countries.forEach { country ->
                        Log.d("DatabaseDebugger", "Country: ${country.code} - ${country.nameKey}")
                    }
                }

            } catch (e: Exception) {
                Log.e("DatabaseDebugger", "Error reading database: ${e.message}", e)
            }
        }
    }

    fun logTableExists() {
        scope.launch {
            try {
                Log.d("DatabaseDebugger", "=== DATABASE STATUS ===")
                Log.d("DatabaseDebugger", "Database path: ${database.openHelper.databaseName}")
                Log.d("DatabaseDebugger", "Database is open: ${database.isOpen}")
                Log.d("DatabaseDebugger", "======================")

            } catch (e: Exception) {
                Log.e("DatabaseDebugger", "Error checking database status: ${e.message}", e)
            }
        }
    }
}