package com.example.android_dev.data

import android.content.Context
import com.example.android_dev.domain.UserAccount
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

// 账户操作结果功能：用密封类清晰表达注册/登录的成功与各类失败原因。
sealed interface AuthResult {
    data class Success(val username: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

// 本地账户仓库功能：负责注册、登录、登出、当前用户读取，密码加盐哈希后存储。
class AccountRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("smart_todo_accounts", Context.MODE_PRIVATE)

    // 当前登录用户名功能：未登录时返回 null。
    fun currentUsername(): String? = prefs.getString(KEY_CURRENT, null)

    fun isLoggedIn(): Boolean = currentUsername() != null

    // 注册功能：校验用户名/密码、查重，成功后落库并自动登录。
    fun register(username: String, password: String): AuthResult {
        val name = username.trim()
        when {
            name.length < 2 -> return AuthResult.Error("用户名至少 2 个字符")
            password.length < 4 -> return AuthResult.Error("密码至少 4 个字符")
            findAccount(name) != null -> return AuthResult.Error("用户名已存在")
        }
        val salt = newSalt()
        val account = UserAccount(
            username = name,
            passwordHash = hash(password, salt),
            salt = salt
        )
        saveAccounts(loadAccounts() + account)
        setCurrent(name)
        return AuthResult.Success(name)
    }

    // 登录功能：按用户名取出盐重新哈希比对密码。
    fun login(username: String, password: String): AuthResult {
        val account = findAccount(username.trim())
            ?: return AuthResult.Error("用户不存在")
        if (hash(password, account.salt) != account.passwordHash) {
            return AuthResult.Error("密码错误")
        }
        setCurrent(account.username)
        return AuthResult.Success(account.username)
    }

    // 游客登录功能：不创建账户，使用专用游客数据分区。
    fun loginAsGuest(): AuthResult {
        setCurrent(GUEST)
        return AuthResult.Success(GUEST)
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT).apply()
    }

    private fun setCurrent(username: String) {
        prefs.edit().putString(KEY_CURRENT, username).apply()
    }

    private fun findAccount(username: String): UserAccount? =
        loadAccounts().firstOrNull { it.username.equals(username, ignoreCase = true) }

    private fun loadAccounts(): List<UserAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val json = array.getJSONObject(index)
                UserAccount(
                    username = json.getString("username"),
                    passwordHash = json.getString("passwordHash"),
                    salt = json.getString("salt"),
                    createdAt = json.optLong("createdAt", System.currentTimeMillis())
                )
            }
        }.getOrElse { emptyList() }
    }

    private fun saveAccounts(accounts: List<UserAccount>) {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("username", account.username)
                    .put("passwordHash", account.passwordHash)
                    .put("salt", account.salt)
                    .put("createdAt", account.createdAt)
            )
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    private fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.toHex()
    }

    // 密码哈希功能：SHA-256(盐 + 密码)，避免明文存储。
    private fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((salt + password).toByteArray(Charsets.UTF_8)).toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        const val GUEST = "_guest"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_CURRENT = "current_user"
    }
}
