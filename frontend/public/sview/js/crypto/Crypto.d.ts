declare let CryptoJS: CryptoJS;
interface CryptoJS{
	AES: AES;
	enc: ENC;
	MD5(str): Result;
	mode: MODE;
	pad: PADDING;
}
interface Result{
	toString(str?): string;
}
interface AES{
    decrypt(str, key?, options?): Result;
    encrypt(str, key?, options?): Result;
}
interface Utf{
	parse(k);
}
interface ENC{
	Utf8: Utf;
}
interface MODE{
	CBC;
}
interface PADDING{
	Pkcs7;
	Pkcs5;
}
