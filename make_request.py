import requests

def print_response(res):
	print("----STATUS----")
	print(res.status_code)

	print("----HEADERS----")
	for header in res.headers:
		print(header, ":", res.headers[header])
	
	print("----BODY----")
	print(res.text)


headers = {
	"Connection": "close",
}

res = requests.get("http://localhost:6789/index.html", headers=headers)
print_response(res)

