package cipher

object XORCipher {

    def encryptOrDecrypt(input: String, key: String): String = {
        val keyInt = key.foldLeft(0) { case (s, c) => s + c.toInt }
        input.map { c => (c ^ keyInt).toChar }
    }

}
