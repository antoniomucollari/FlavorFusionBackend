import re

with open('to_upload.api', 'r') as f:
    content = f.read()

pattern = r'[a-f0-9\-]{36}_[A-Za-z0-9_]+\.jpeg\.png'

counter = 1
def replace_match(m):
    global counter
    result = f'{counter}.jpeg.png'
    counter += 1
    return result

content = re.sub(pattern, replace_match, content)

with open('to_upload_fixed.api', 'w') as f:
    f.write(content)

print(f"Replaced {counter - 1} filenames")