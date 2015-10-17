package rocklee.security;


import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;



public class DESUtil {

    public static final String KEY_ALGORITHM = "DESede";

    public static final String CIPHER_ALGORITHM = "DESede/ECB/PKCS5Padding";
    
    public static final String CHAR_SET="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    
    public static String randomString(int size)
    {
    	Random random=new Random();
    	StringBuffer str_buff=new StringBuffer();
    	char tmp='\0';
    	
    	for (int i = 0; i < size; i++)
		{
    		tmp=CHAR_SET.charAt(random.nextInt(CHAR_SET.length()));
    		str_buff.append(tmp);
		}
    	return str_buff.toString();
    }
    

    /**
     *   
     * ������Կkey����
     * @param KeyStr ��Կ�ַ��� 
     * @return ��Կ���� 
     * @throws InvalidKeyException   
     * @throws NoSuchAlgorithmException   
     * @throws InvalidKeySpecException   
     * @throws Exception 
     */
    private static SecretKey keyGenerator(String keyStr) throws Exception {
        byte input[] = HexString2Bytes(keyStr);
        DESedeKeySpec desKey = new DESedeKeySpec(input);
        //����һ���ܳ׹�����Ȼ��������DESKeySpecת����
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        SecretKey securekey = keyFactory.generateSecret(desKey);
        return securekey;
    }

    private static int parse(char c) {
        if (c >= 'a') return (c - 'a' + 10) & 0x0f;
        if (c >= 'A') return (c - 'A' + 10) & 0x0f;
        return (c - '0') & 0x0f;
    }

    public static byte[] HexString2Bytes(String hexstr) {
        byte[] b = new byte[hexstr.length() / 2];
        int j = 0;
        for (int i = 0; i < b.length; i++) {
            char c0 = hexstr.charAt(j++);
            char c1 = hexstr.charAt(j++);
            b[i] = (byte) ((parse(c0) << 4) | parse(c1));
        }
        return b;
    }

    /** 
     * ��������
     * @param data ����������
     * @param key ��Կ
     * @return ���ܺ������ 
     */
    public static String encrypt(String data, String key) throws Exception {
        Key deskey = keyGenerator(key);
        // ʵ����Cipher�������������ʵ�ʵļ��ܲ���
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecureRandom random = new SecureRandom();
        // ��ʼ��Cipher��������Ϊ����ģʽ
        cipher.init(Cipher.ENCRYPT_MODE, deskey, random);
        byte[] results = cipher.doFinal(data.getBytes());
//        for (int i = 0; i < results.length; i++) {
//            System.out.print(results[i] + " ");
//        }
        System.out.println();
        // ִ�м��ܲ��������ܺ�Ľ��ͨ��������Base64������д��� 
        return Base64.getEncoder().encodeToString(results);
    }

    /** 
     * �������� 
     * @param data ���������� 
     * @param key ��Կ 
     * @return ���ܺ������ 
     */
    public static String decrypt(String data, String key) throws Exception {
        Key deskey = keyGenerator(key);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        //��ʼ��Cipher��������Ϊ����ģʽ
        cipher.init(Cipher.DECRYPT_MODE, deskey);
        // ִ�н��ܲ���
        return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
    }

    public static void main(String[] args) throws Exception {
    	String msg = "what do you have in mind?";
		msg+=" Tooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo Long to get a proper output?!?!?!?";
		
        System.out.println("ԭ��: " + msg);
        String key = "A1B2C3D4E5F60708A1B2C3D4E5F60708A1B2C3D4E5F60708A1B2C3D4E5F60708";
        String encryptData = encrypt(msg, key);
        System.out.println("���ܺ�: " + encryptData);
        String decryptData = decrypt(encryptData, key);
        System.out.println("���ܺ�: " + decryptData);
    }
}