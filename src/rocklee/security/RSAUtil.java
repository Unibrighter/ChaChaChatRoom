package rocklee.security;

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

	public static KeyPair generateRSAKeyPair(String filePath)
	{
		// KeyPairGenerator ���������ɹ�Կ��˽Կ�ԣ�����RSA�㷨���ɶ���
		KeyPairGenerator keyPairGen = null;
		try
		{
			keyPairGen = KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1)
		{
			e1.printStackTrace();
		}
		// ��ʼ����Կ��������,��Կ��СΪ1024λ
		keyPairGen.initialize(1024);
		// ����һ����Կ�ԣ�������keyPair��
		KeyPair keyPair = keyPairGen.generateKeyPair();
		// �õ�˽Կ
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		// �õ���Կ
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
				// Cipher������ɼ��ܻ���ܹ���������RSA
				Cipher cipher = Cipher.getInstance("RSA");
				// ���ݹ�Կ����Cipher������г�ʼ��
				cipher.init(Cipher.ENCRYPT_MODE, publicKey);
				// ���ܣ���������resultBytes
				byte[] resultBytes = cipher.doFinal(data.getBytes());
				return encoder.encodeToString(resultBytes);
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
				// ����˽Կ����Cipher������г�ʼ��
				cipher.init(Cipher.DECRYPT_MODE, privateKey);
				// ���ܣ���������resultBytes
				byte[] decBytes = cipher.doFinal(decoder.decode(data));
				return new String(decBytes);
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
				// Cipher������ɼ��ܻ���ܹ���������RSA
				Cipher cipher = Cipher.getInstance("RSA");
				// ���ݹ�Կ����Cipher������г�ʼ��
				cipher.init(Cipher.ENCRYPT_MODE, privateKey);
				// ���ܣ���������resultBytes
				byte[] resultBytes = cipher.doFinal(data.getBytes());
				return encoder.encodeToString(resultBytes);
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
				// ����˽Կ����Cipher������г�ʼ��
				cipher.init(Cipher.DECRYPT_MODE, publicKey);
				// ���ܣ���������resultBytes
				byte[] decBytes = cipher.doFinal(decoder.decode(data));
				return new String(decBytes);
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

			// �õ�һ��MD5ת�����������ҪSHA1�������ɡ�SHA1����

			MessageDigest messageDigest = MessageDigest.getInstance("MD5");

			// ������ַ���ת�����ֽ�����

			byte[] inputByteArray = input.getBytes();

			// inputByteArray�������ַ���ת���õ����ֽ�����

			messageDigest.update(inputByteArray);

			// ת�������ؽ����Ҳ���ֽ����飬����16��Ԫ��

			byte[] resultByteArray = messageDigest.digest();

			// �ַ�����ת�����ַ�������

			return byteArrayToHex(resultByteArray);

		} catch (NoSuchAlgorithmException e)
		{

			return null;

		}
	}

	private static String byteArrayToHex(byte[] byteArray)
	{
		// ���ȳ�ʼ��һ���ַ����飬�������ÿ��16�����ַ�

		char[] hexDigits =
		{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
				'E', 'F' };

		// newһ���ַ����飬�������������ɽ���ַ����ģ�����һ�£�һ��byte�ǰ�λ�����ƣ�Ҳ����2λʮ�������ַ���2��8�η�����16��2�η�����

		char[] resultCharArray = new char[byteArray.length * 2];

		// �����ֽ����飬ͨ��λ���㣨λ����Ч�ʸߣ���ת�����ַ��ŵ��ַ�������ȥ

		int index = 0;

		for (byte b : byteArray)
		{

			resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];

			resultCharArray[index++] = hexDigits[b & 0xf];

		}
		// �ַ�������ϳ��ַ�������
		return new String(resultCharArray);
	}

	// RSA���ܽ���
	public static void main(String[] args)
	{
		try
		{
			String msg = "���˻�_��ȫ��̼���";
			System.out.println("������:" + msg);
			// KeyPairGenerator ���������ɹ�Կ��˽Կ�ԣ�����RSA�㷨���ɶ���
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
			// ��ʼ����Կ��������,��Կ��СΪ1024λ
			keyPairGen.initialize(1024);
			// ����һ����Կ�ԣ�������keyPair��
			KeyPair keyPair = keyPairGen.generateKeyPair();
			// �õ�˽Կ
			RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
			// �õ���Կ
			RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
			// �ù�Կ����
			String result = RSAUtil.encryptUsingPublicKey(publicKey, msg);

			System.out.println("�ù�Կ���ܺ�������:" + result);

			// ��˽Կ����
			String dec = RSAUtil.decryptUsingPrivateKey(privateKey, result);
			System.out.println("��˽Կ���ܺ�����:" + dec);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

}