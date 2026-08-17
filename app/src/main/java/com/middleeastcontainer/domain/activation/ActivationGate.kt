package com.middleeastcontainer.domain.activation

import com.middleeastcontainer.data.session.SecurePrefs
import timber.log.Timber
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * First-run activation.
 *
 * Android does not allow an app to demand a password during installation — that
 * is the operating system's package installer, which an APK cannot alter. This
 * is the next best thing: the app installs, but stays unusable until the code is
 * entered, so a copy of the APK on its own is worth nothing.
 *
 * The password is never stored. Only a PBKDF2 hash is compiled in, because the
 * strings inside an APK are trivially readable — a plaintext code would be found
 * in seconds by anyone who cared to look.
 *
 * Be clear about what this is: a deterrent against the APK being passed around
 * and used, not protection against someone who can modify the app. Anyone able
 * to patch the bytecode can remove the check entirely.
 */
@Singleton
class ActivationGate @Inject constructor(
    private val securePrefs: SecurePrefs,
) {

    val isActivated: Boolean
        get() = securePrefs.prefs.getBoolean(KEY_ACTIVATED, false)

    /**
     * Checks the code and remembers success.
     *
     * @return true when the code was correct.
     */
    fun activate(code: String): Boolean {
        val ok = matches(code.trim())
        if (ok) {
            securePrefs.prefs.edit().putBoolean(KEY_ACTIVATED, true).apply()
            Timber.i("Activated")
        } else {
            Timber.w("Activation refused")
        }
        return ok
    }

    private fun matches(candidate: String): Boolean {
        if (candidate.isEmpty()) return false
        val spec = PBEKeySpec(
            candidate.toCharArray(),
            SALT.toByteArray(Charsets.UTF_8),
            ITERATIONS,
            KEY_BITS,
        )
        val computed = SecretKeyFactory
            .getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        // Constant-time compare: a length-or-first-difference exit would leak
        // information about the expected value through timing.
        return MessageDigest.isEqual(computed, EXPECTED)
    }

    private companion object {
        const val KEY_ACTIVATED = "activation_complete"

        /**
         * PBKDF2 of the activation code. Deliberately not the code itself: the
         * strings in an APK can be read with a single command.
         */
        val EXPECTED: ByteArray = android.util.Base64.decode(
            "R/eGrj4aTfw8a/YfR9Ur15qK+MVd302goH7SNppFz6Y=",
            android.util.Base64.DEFAULT,
        )

        /** Not secret — its job is to defeat precomputed tables. */
        const val SALT = "mecrc-activation-v1"

        /** High enough that guessing is slow, low enough not to stall the screen. */
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
