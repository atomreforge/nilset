package net.atomreforge.nilset.ui.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RegisterFormValidatorTest {
    private fun validPassword() = "abcdef"

    private fun validForm(
        username: String = "alice",
        nickname: String = "Alice",
        password: String = validPassword(),
        confirmPassword: String = password,
        registerCode: String = "ABCD." + "0".repeat(128),
    ) = RegisterFormValidator.validate(
        username = username,
        nickname = nickname,
        password = password,
        confirmPassword = confirmPassword,
        registerCode = registerCode,
    )

    @Test
    fun `accepts valid registration form`() {
        assertNull(validForm())
    }

    @Test
    fun `rejects invalid username`() {
        assertEquals("用户名需为1-15位字母或数字", validForm(username = "alice-1"))
        assertEquals("用户名需为1-15位字母或数字", validForm(username = "a".repeat(16)))
    }

    @Test
    fun `rejects nickname outside unicode character limit`() {
        assertEquals("昵称需为1-15个字符", validForm(nickname = "字".repeat(16)))
    }

    @Test
    fun `rejects password mismatch and short password`() {
        assertEquals("两次输入的密码不一致", validForm(confirmPassword = "abcdefg"))
        assertEquals("密码需为6-128个字符", validForm(password = "12345", confirmPassword = "12345"))
    }

    @Test
    fun `rejects malformed register code`() {
        assertEquals("注册码格式不正确", validForm(registerCode = "abc.def"))
        assertEquals("注册码格式不正确", validForm(registerCode = "ABCD." + "0".repeat(127)))
    }
}
