import requests

def print_response(res):
	print("----STATUS----")
	print(res.status_code)

	print("----HEADERS----")
	for header in res.headers:
		print(header, ":", res.headers[header])
	
	print("----BODY----")
	print(res.text)


computer_headers = {
	"Connection": "close",
	"If-Modified-Since": "Wed, 21 Oct 2024 07:28:00 GMT",
	"User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:60.0) Gecko/20100101 Firefox/60.0"
}

mobile_headers = {
	"Connection": "close",
	"User-Agent": "Mozilla/5.0 (Linux; Android 8.0.0; SM-G930F Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Mobile Safari/537.36"
}

res = requests.get("http://localhost:6789/", headers=mobile_headers)
print_response(res)

