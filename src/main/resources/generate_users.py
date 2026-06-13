import os
import math

# We are going to generate 1000 users starting from id 569 up to 1568
START_ID = 569
NUM_USERS = 1000

desktop_path = r"C:\Users\anton\OneDrive\Desktop"
output_file = os.path.join(desktop_path, "users_569_1568.sql")

users_columns = [
    "id",
    "address",
    "created_at",
    "email",
    "is_active",
    "name",
    "password",
    "phone_number",
    "profile_url",
    "require_password_change",
    "token_version",
    "updated_at",
    "verification_status",
    "deliver_location_id",
    "last_selected_payment_method_id",
    "created_by_company"
]

tirana_places = [
    ("Sheshi Skënderbej, Tiranë 1001, Albania", "skenderbej"),
    ("Blloku, Rruga Ibrahim Rugova, Tiranë 1001, Albania", "blloku"),
    ("Parku i Madh i Liqenit, Tiranë 1001, Albania", "parkulias"),
    ("Piramida e Tiranës, Bulevardi Dëshmorët e Kombit, Tiranë 1001, Albania", "piramida"),
    ("Shtëpia me Gjethe, Rruga Ibrahim Rugova, Tiranë 1001, Albania", "gjethet"),
    ("Qendra Toptani, Rruga Abdi Toptani, Tiranë 1001, Albania", "toptani"),
    ("Pazari i Ri, Rruga Shenasi Dishnica, Tiranë 1001, Albania", "pazariri"),
    ("Rruga Myslym Shyri, Tiranë 1001, Albania", "myslymshyri"),
    ("Rruga e Kavajës, Tiranë 1001, Albania", "kavajes"),
    ("Rruga e Durrësit, Tiranë 1001, Albania", "durresit"),
    ("Rruga e Elbasanit, Tiranë 1001, Albania", "elbasanit"),
    ("Rruga e Barrikadave, Tiranë 1001, Albania", "barrikadave"),
    ("Rruga e Dibrës, Tiranë 1001, Albania", "dibres"),
    ("Air Albania Stadium, Rruga Lekë Dukagjini, Tiranë 1001, Albania", "airalbania"),
    ("Dajti Express, Rruga Mahmut Allushi, Tiranë 1011, Albania", "dajtixpress"),
    ("Bunk'Art 1, Rruga Fadil Deliu, Tiranë 1011, Albania", "bunkart1"),
    ("Bunk'Art 2, Rruga Abdi Toptani, Tiranë 1001, Albania", "bunkart2"),
    ("Tirana Ring Center, Rruga Don Bosko, Tiranë 1001, Albania", "ringcenter"),
    ("Zogu i Zi, Rruga Dritan Hoxha, Tiranë 1001, Albania", "zoguizi"),
    ("Kinostudio, Rruga Aleksandër Moisiu, Tiranë 1001, Albania", "kinostudio"),
    ("Kombinati, Rruga Llazi Miho, Tiranë 1001, Albania", "kombinat"),
    ("Lapraka, Rruga Dritan Hoxha, Tiranë 1001, Albania", "laprake"),
    ("Don Bosko, Rruga Don Bosko, Tiranë 1001, Albania", "donbosko"),
    ("Ali Demi, Rruga Ali Demi, Tiranë 1001, Albania", "alidemi"),
    ("Kodra e Diellit, Rruga e Qershive, Tiranë 1001, Albania", "kodradiellit"),
    ("Selitë, Rruga Irfan Tomini, Tiranë 1001, Albania", "selite"),
    ("Sauk, Rruga e Elbasanit, Tiranë 1001, Albania", "sauk"),
    ("Shkozë, Rruga e Shkozës, Tiranë 1001, Albania", "shkoze"),
    ("Astir, Rruga Teodor Keko, Tiranë 1001, Albania", "astir"),
    ("Yzberisht, Rruga Tre Dëshmorët, Tiranë 1001, Albania", "yzberisht"),
    ("Rruga Ndre Mjeda, Tiranë 1001, Albania", "ndremjeda"),
    ("Rruga Frosina Plaku, Tiranë 1001, Albania", "frosinaplaku"),
    ("Rruga Muhamet Gjollesha, Tiranë 1001, Albania", "muhametgjollesha"),
    ("Rruga Medar Shtylla, Tiranë 1001, Albania", "medarshtylla"),
    ("Rruga Sulejman Delvina, Tiranë 1001, Albania", "sulejmandelvina"),
    ("Rruga Abdyl Frashëri, Tiranë 1001, Albania", "abdylfrasheri"),
    ("Rruga Sami Frashëri, Tiranë 1001, Albania", "samifrasheri"),
    ("Rruga Vaso Pasha, Tiranë 1001, Albania", "vasopasha"),
    ("Rruga Ismail Qemali, Tiranë 1001, Albania", "ismailqemali"),
    ("Rruga Brigada VIII, Tiranë 1001, Albania", "brigadaviii"),
    ("Rruga Pjetër Bogdani, Tiranë 1001, Albania", "pjeterbogdani"),
    ("Rruga Gjergj Fishta, Tiranë 1001, Albania", "gjergjfishta"),
    ("Rruga e Kavajës, pranë Pallatit me Shigjeta, Tiranë 1001, Albania", "shigjetat"),
    ("Rruga Hoxha Tahsim, Tiranë 1001, Albania", "hoxhatahsim"),
    ("Rruga Luigj Gurakuqi, Tiranë 1001, Albania", "luigjgurakuqi"),
    ("Rruga George W. Bush, Tiranë 1001, Albania", "georgebush"),
    ("Rruga Urani Pano, Tiranë 1001, Albania", "uranipano"),
    ("Rruga Ded Gjo Luli, Tiranë 1001, Albania", "dedgjoluli"),
    ("Rruga Çamëria, Tiranë 1001, Albania", "cameria"),
    ("Rruga Ibrahim Dervishi, Tiranë 1001, Albania", "ibrahimdervishi"),
    ("Rruga Nikolollë Jorga, Tiranë 1001, Albania", "nikolljorga"),
    ("Rruga Papa Gjon Pali II, Tiranë 1001, Albania", "papagjonpali"),
    ("Rruga Asim Zeneli, Tiranë 1001, Albania", "asimzeneli"),
    ("Rruga Qamil Guranjaku, Tiranë 1001, Albania", "qamilguranjaku"),
    ("Rruga Themistokli Gërmenji, Tiranë 1001, Albania", "themistokli"),
    ("Rruga Faik Konica, Tiranë 1001, Albania", "faikkonica"),
    ("Rruga Jul Variboba, Tiranë 1001, Albania", "julvariboba"),
    ("Rruga e Elbasanit, pranë Ambasadës Amerikane, Tiranë 1001, Albania", "ambasadausa"),
    ("Rruga Mihal Grameno, Tiranë 1001, Albania", "mihalgrameno"),
    ("Rruga Petro Nini Luarasi, Tiranë 1001, Albania", "petronini"),
    ("Rruga Shefqet Kuka, Tiranë 1001, Albania", "shefqetkuka"),
    ("Rruga Arkitekt Sinani, Tiranë 1001, Albania", "arkitektsinani"),
    ("Rruga e Dibrës, pranë QSUT, Tiranë 1001, Albania", "qsut"),
    ("Rruga Bardhyl, Tiranë 1001, Albania", "bardhyl"),
    ("Rruga Kongresi i Manastirit, Tiranë 1001, Albania", "kongresimanastirit"),
    ("Rruga Siri Kodra, Tiranë 1001, Albania", "sirikodra"),
    ("Rruga Haxhi Hysen Dalliu, Tiranë 1001, Albania", "haxhidalliu"),
    ("Rruga Reshit Petrela, Tiranë 1001, Albania", "reshitpetrela"),
    ("Rruga Mine Peza, Tiranë 1001, Albania", "minepeza"),
    ("Rruga Jordan Misja, Tiranë 1001, Albania", "jordanmisja"),
    ("Rruga Karl Gega, Tiranë 1001, Albania", "karlgega"),
    ("Rruga Vangjel Noti, Tiranë 1001, Albania", "vangjelnoti"),
    ("Rruga Lord Bajron, Tiranë 1001, Albania", "lordbajron"),
    ("Rruga Gjergj Legisi, Tiranë 1001, Albania", "gjergjlegisi"),
    ("Rruga Pandi Dari, Tiranë 1001, Albania", "pandidari"),
    ("Rruga Mikel Maruli, Tiranë 1001, Albania", "mikelmaruli"),
    ("Rruga Loni Ligori, Tiranë 1001, Albania", "loniligori"),
    ("Rruga Tom Puka, Tiranë 1001, Albania", "tompuka"),
    ("Rruga Aleksandri i Madh, Tiranë 1001, Albania", "aleksandrimadh"),
    ("Rruga e Qelqit, Tiranë 1001, Albania", "rrugaqelqit"),
    ("Rruga Tanush Shyti, Tiranë 1001, Albania", "tanushshyti"),
    ("Rruga Sali Butka, Tiranë 1001, Albania", "salibutka"),
    ("Rruga Hamdi Cenoimeri, Tiranë 1001, Albania", "hamdicenoimeri"),
    ("Rruga Besim Alla, Tiranë 1001, Albania", "besimalla"),
    ("Rruga Sadik Petrela, Tiranë 1001, Albania", "sadikpetrela"),
    ("Rruga e Arbërit, Tiranë 1001, Albania", "rrugaarberit"),
    ("Rruga Kujtim Laro, Tiranë 1001, Albania", "kujtimlaro"),
    ("Rruga Kristo Luarasi, Tiranë 1001, Albania", "kristoluarasi"),
    ("Rruga Liman Kaba, Tiranë 1001, Albania", "limankaba"),
    ("Rruga Koliqi, Tiranë 1001, Albania", "koliqi"),
    ("Rruga Riza Cuka, Tiranë 1001, Albania", "rizacuka"),
    ("Rruga Todi Shkurti, Tiranë 1001, Albania", "todishkurti"),
    ("Rruga Zenel Bastari, Tiranë 1001, Albania", "zenelbastari"),
    ("Rruga Qamil Guranjaku, Tiranë 1001, Albania", "guranjaku"),
    ("Rruga Mustafa Matohiti, Tiranë 1001, Albania", "mustafamatohiti"),
    ("Rruga e Kosovarëve, Tiranë 1001, Albania", "kosovareve"),
    ("Rruga e Bogdaneve, Tiranë 1001, Albania", "bogdaneve"),
    ("Rruga Kont Urani, Tiranë 1001, Albania", "konturani"),
    ("Rruga Mihal Duri, Tiranë 1001, Albania", "mihalduri"),
    ("Rruga e Himares, Tiranë 1001, Albania", "himares")
]

assert len(tirana_places) == 100, f"Expected 100 locations, got {len(tirana_places)}"

users_inserts = []
for i in range(NUM_USERS):
    user_id = START_ID + i
    email = f"customerFlavorFusionTest{user_id}@gmail.com"
    name = email
    
    val_str = (
        f"({user_id}, "
        f"'Yzberisht, Rruga tre deshmoret', "
        f"'2026-04-02 15:05:07.546098', "
        f"'{email}', "
        f"true, "
        f"'{name}', "
        f"'$2a$10$CG2pAzSgv1tPmmSW0A9IIuWKK5uhQ2iosNMp0bjgBDwYfSKPUQfCG', "
        f"'0123456789', "
        f"'https://d3u269mlo8clta.cloudfront.net/user-profile-image/3.png', "
        f"false, "
        f"1, "
        f"NULL, "
        f"NULL, "
        f"NULL, "
        f"3, "
        f"NULL)"
    )
    users_inserts.append(val_str)

# We also need delivery location IDs. Assuming we just continue from some high number or offset
# We'll use id starting from 520 to avoid duplicate keys
DL_START_ID = 520

dl_inserts = []
for i in range(NUM_USERS):
    dl_id = DL_START_ID + i
    user_id = START_ID + i
    loc_idx = i % 100
    loc_name, loc_nickname = tirana_places[loc_idx]
    
    lat_offset = 0.015 * math.sin(loc_idx * 0.2) + 0.005 * math.cos(loc_idx * 0.5)
    lng_offset = 0.02 * math.cos(loc_idx * 0.2) - 0.005 * math.sin(loc_idx * 0.5)
    
    latitude = round(41.3275432 + lat_offset, 7)
    longitude = round(19.8188123 + lng_offset, 7)
    
    val_str = (
        f"({dl_id}, "
        f"{latitude}, "
        f"'{loc_name.replace("'", "''")}', "
        f"{longitude}, "
        f"'{loc_nickname}', "
        f"{user_id})"
    )
    dl_inserts.append(val_str)

ur_inserts = []

# Add new user roles (all getting customer role 2)
for i in range(NUM_USERS):
    user_id = START_ID + i
    val_str = f"({user_id}, 2)"
    ur_inserts.append(val_str)

sql_content = "-- Start a transaction for safety\nBEGIN;\n\n"

sql_content += f"-- 1. Insert {NUM_USERS} new users with deliver_location_id as NULL to respect foreign keys\n"
sql_content += f"INSERT INTO users (\n    {', '.join(users_columns)}\n) VALUES\n" + ",\n".join(users_inserts) + ";\n\n"

sql_content += f'-- 2. Insert {NUM_USERS} new delivery locations (100 distinct places, each shared by 10 users)\n'
sql_content += 'INSERT INTO "delivery-location" (\n    id, latitude, location_name, longitude, nickname, user_id\n) VALUES\n' + ",\n".join(dl_inserts) + ";\n\n"

sql_content += f"-- 3. Insert {NUM_USERS} new users_roles mappings with role CUSTOMER (role_id = 2)\n"
sql_content += "INSERT INTO users_roles (\n    user_id, role_id\n) VALUES\n" + ",\n".join(ur_inserts) + ";\n\n"

sql_content += "-- 4. Link back the users to their respective deliver_location_id\n"
sql_content += f"UPDATE users SET deliver_location_id = id - {START_ID - DL_START_ID} WHERE id BETWEEN {START_ID} AND {START_ID + NUM_USERS - 1};\n\n"

sql_content += "-- Commit the transaction\nCOMMIT;\n"

with open(output_file, "w", encoding="utf-8") as f:
    f.write(sql_content)

print(f"Successfully generated advanced SQL scripts for {NUM_USERS} users starting from ID {START_ID}.")
