package rocklee.security;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.Cipher;

public class RSAUtil
{
	private static Decoder decoder = Base64.getDecoder();
	private static Encoder encoder = Base64.getEncoder();
	
    private static final int MAX_ENCRYPT_BLOCK = 117;  
    private static final int MAX_DECRYPT_BLOCK = 128;  

	public static KeyPair generateRSAKeyPair(String filePath)
	{
		// KeyPairGenerator 类用于生成公钥和私钥对，基于RSA算法生成对象
		KeyPairGenerator keyPairGen = null;
		try
		{
			keyPairGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1)
		{
			e1.printStackTrace();
		}
		// 初始化密钥对生成器,密钥大小为1024位
		keyPairGen.initialize(1024);
		// 生成一个密钥对，保存在keyPair中
		KeyPair keyPair = keyPairGen.generateKeyPair();
		// 得到私钥
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		// 得到公钥
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

		try
		{
			ObjectOutputStream oos_public_rsa = new ObjectOutputStream(
					new FileOutputStream(filePath));
			oos_public_rsa.writeObject(publicKey);
			oos_public_rsa.flush();
			oos_public_rsa.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		return keyPair;

	}

	public static RSAPublicKey getPublicKeyFromFile(String path)
	{
		RSAPublicKey publicKey = null;
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					path));
			publicKey = (RSAPublicKey) ois.readObject();
			ois.close();

		} catch (IOException e)
		{
			System.err.println("Something wrong with loading the public key from file!");
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{

			e.printStackTrace();
		}
		return publicKey;

	}

	// public static RSAPrivateKey getPrivateKey()
	// {
	//
	// }

	public static String encryptUsingPublicKey(RSAPublicKey publicKey,
			String data)
	{
		if (publicKey != null)
		{
			try
			{
				// Cipher负责完成加密或解密工作，基于RSA
				Cipher cipher = Cipher.getInstance("RSA");
				// 根据公钥，对Cipher对象进行初始化
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				// 加密，结果保存进resultBytes
				
				byte[] input_data= data.getBytes();
				//待处理数据的长度
				int inputLen = input_data.length;
				//缓冲流,作为暂存的buffer
		        ByteArrayOutputStream out = new ByteArrayOutputStream();  
		        //游标,指示已经加密/解密到哪个位置
		        int offSet = 0;
		        byte[] cache;  
		        int i = 0;  
		        // 对数据分段解密  
		        while (inputLen - offSet > 0) {  
		            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
		                cache = cipher.doFinal(input_data, offSet, MAX_ENCRYPT_BLOCK);  
		            } else {  
		                cache = cipher.doFinal(input_data, offSet, inputLen - offSet);  
		            }  
		            out.write(cache, 0, cache.length);  
		            i++;  
		            offSet = i * MAX_ENCRYPT_BLOCK;  
		        }  
		        byte[] decryptedData = out.toByteArray();  
		        out.close();  
				return encoder.encodeToString(decryptedData);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String decryptUsingPrivateKey(RSAPrivateKey privateKey,
			String data)
	{
		if (privateKey != null)
		{
			try
			{
				Cipher cipher = Cipher.getInstance("RSA");
				// 根据私钥，对Cipher对象进行初始化
				cipher.init(Cipher.DECRYPT_MODE, privateKey);

				byte[] input_data= decoder.decode(data);
				//待处理数据的长度
				int inputLen = input_data.length;
				//缓冲流,作为暂存的buffer
		        ByteArrayOutputStream out = new ByteArrayOutputStream();  
		        //游标,指示已经加密/解密到哪个位置
		        int offSet = 0;
		        byte[] cache;  
		        int i = 0;  
		        // 对数据分段解密  
		        while (inputLen - offSet > 0) {  
		            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
		                cache = cipher.doFinal(input_data, offSet, MAX_DECRYPT_BLOCK);  
		            } else {  
		                cache = cipher.doFinal(input_data, offSet, inputLen - offSet);  
		            }  
		            out.write(cache, 0, cache.length);  
		            i++;  
		            offSet = i * MAX_DECRYPT_BLOCK;  
		        }  
		        byte[] decryptedData = out.toByteArray();  
		        out.close();  
				return new String(decryptedData);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String encryptUsingPrivateKey(RSAPrivateKey privateKey,
			String data)
	{
		if (privateKey != null)
		{
			try
			{
				// Cipher负责完成加密或解密工作，基于RSA
				Cipher cipher = Cipher.getInstance("RSA");
				// 根据公钥，对Cipher对象进行初始化
				cipher.init(Cipher.ENCRYPT_MODE, privateKey);
				// 加密，结果保存进resultBytes
				

				byte[] input_data= data.getBytes();
				//待处理数据的长度
				int inputLen = input_data.length;
				//缓冲流,作为暂存的buffer
		        ByteArrayOutputStream out = new ByteArrayOutputStream();  
		        //游标,指示已经加密/解密到哪个位置
		        int offSet = 0;
		        byte[] cache;  
		        int i = 0;  
		        // 对数据分段解密  
		        while (inputLen - offSet > 0) {  
		            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
		                cache = cipher.doFinal(input_data, offSet, MAX_ENCRYPT_BLOCK);  
		            } else {  
		                cache = cipher.doFinal(input_data, offSet, inputLen - offSet);  
		            }  
		            out.write(cache, 0, cache.length);  
		            i++;  
		            offSet = i * MAX_ENCRYPT_BLOCK;  
		        }  
		        byte[] decryptedData = out.toByteArray();  
		        out.close();  
				return encoder.encodeToString(decryptedData);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String decryptUsingPublicKey(RSAPublicKey publicKey,
			String data)
	{
		if (publicKey != null)
		{
			try
			{
				Cipher cipher = Cipher.getInstance("RSA");
				// 根据私钥，对Cipher对象进行初始化
				cipher.init(Cipher.DECRYPT_MODE, publicKey);

				byte[] input_data= decoder.decode(data);
				//待处理数据的长度
				int inputLen = input_data.length;
				//缓冲流,作为暂存的buffer
		        ByteArrayOutputStream out = new ByteArrayOutputStream();  
		        //游标,指示已经加密/解密到哪个位置
		        int offSet = 0;
		        byte[] cache;  
		        int i = 0;  
		        // 对数据分段解密  
		        while (inputLen - offSet > 0) {  
		            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
		                cache = cipher.doFinal(input_data, offSet, MAX_DECRYPT_BLOCK);  
		            } else {  
		                cache = cipher.doFinal(input_data, offSet, inputLen - offSet);  
		            }  
		            out.write(cache, 0, cache.length);  
		            i++;  
		            offSet = i * MAX_DECRYPT_BLOCK;  
		        }  
		        byte[] decryptedData = out.toByteArray();  
		        out.close();  
				return new String(decryptedData);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	public static String stringMD5(String input)
	{
		try
		{

			// 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）

			MessageDigest messageDigest = MessageDigest.getInstance("MD5");

			// 输入的字符串转换成字节数组

			byte[] inputByteArray = input.getBytes();

			// inputByteArray是输入字符串转换得到的字节数组

			messageDigest.update(inputByteArray);

			// 转换并返回结果，也是字节数组，包含16个元素

			byte[] resultByteArray = messageDigest.digest();

			// 字符数组转换成字符串返回

			return byteArrayToHex(resultByteArray);

		} catch (NoSuchAlgorithmException e)
		{

			return null;

		}
	}

	private static String byteArrayToHex(byte[] byteArray)
	{
		// 首先初始化一个字符数组，用来存放每个16进制字符

		char[] hexDigits =
		{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
				'E', 'F' };

		// new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））

		char[] resultCharArray = new char[byteArray.length * 2];

		// 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去

		int index = 0;

		for (byte b : byteArray)
		{

			resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];

			resultCharArray[index++] = hexDigits[b & 0xf];

		}
		// 字符数组组合成字符串返回
		return new String(resultCharArray);
	}

	// RSA加密解密
	public static void main(String[] args)
	{
		try
		{
			String msg = "what do you have in mind?";
			msg+=" Toooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo Long to get a proper output?!?!?!?";
			System.out.println("Plain Text :\t" + msg);
			// KeyPairGenerator 类用于生成公钥和私钥对，基于RSA算法生成对象
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
			// 初始化密钥对生成器,密钥大小为1024位
			keyPairGen.initialize(1024);
			// 生成一个密钥对，保存在keyPair中
			KeyPair keyPair = keyPairGen.generateKeyPair();
			// 得到私钥
			RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
			// 得到公钥
			RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
			// 用公钥加密
			String result = RSAUtil.encryptUsingPublicKey(publicKey, msg);

			System.out.println("Cipher Text :\t" + result);

			// 用私钥解密
			String dec = RSAUtil.decryptUsingPrivateKey(privateKey, result);
			System.out.println("After Decryption:\t" + dec);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}