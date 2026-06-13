import json
import os
import random
import glob
import sys
import argparse

def process_restaurant(folder_path, user_seq, rest_seq, loc_seq, cat_seq, og_seq, ov_seq, menu_seq, bmi_seq, description_param=None):
    # Determine restaurant name from the primary JSON file (e.g., )
    # First, collect candidate JSON files (excluding master.json and assortment files)
    candidate_jsons = [f for f in glob.glob(os.path.join(folder_path, "*.json"))
                      if os.path.basename(f) not in ("master.json",) and not os.path.basename(f).startswith("assortment-")]
    if candidate_jsons:
        primary_json = candidate_jsons[0]
        restaurant_name = os.path.splitext(os.path.basename(primary_json))[0]
    else:
        # Fallback to folder name if no specific JSON file is found
        restaurant_name = os.path.basename(os.path.normpath(os.path.abspath(folder_path)))
    # Use restaurant_name for naming throughout the script
    folder_name = restaurant_name
    
    # Read master.json if it exists
    master_path = os.path.join(folder_path, 'master.json')
    venues = {}
    if os.path.exists(master_path):
        with open(master_path, 'r', encoding='utf-8', errors='replace') as f:
            master_data = json.load(f)
            
        # Extract venues from master.json
        for section in master_data.get('sections', []):
            for item in section.get('items', []):
                if 'venue' in item:
                    v = item['venue']
                    venues[v['slug']] = v
    else:
        print(f"Notice: master.json not found in {folder_path}. Proceeding with default single branch settings.")
        
    # Find all assortment files
    assort_files = glob.glob(os.path.join(folder_path, 'assortment-*.json'))
    if not assort_files:
        # Fallback: check for a generic assortment.json file
        generic_path = os.path.join(folder_path, 'assortment.json')
        if os.path.exists(generic_path):
            assort_files = [generic_path]
        else:
            # Fallback: check for any JSON file in the folder (excluding master.json if present)
            all_jsons = glob.glob(os.path.join(folder_path, '*.json'))
            all_jsons = [f for f in all_jsons if os.path.basename(f) != 'master.json']
            if all_jsons:
                assort_files = [all_jsons[0]]  # Assume the single available JSON file is the branch assortment
            else:
                print(f"Error: no assortment JSON files found in {folder_path}")
                return
        
    branches_info = []
    for af in assort_files:
        filename = os.path.basename(af)
        # e.g. assortment-burger-king-bllok.json -> burger-king-bllok, assortment.json -> assortment
        slug = filename.replace('assortment-', '').replace('.json', '')
        if slug in venues:
            v = venues[slug]
            branches_info.append({
                'slug': slug,
                'file_path': af,
                'name': v['name'],
                'address': v.get('address', 'Tirana'),
                'location': v.get('location', [19.818, 41.320])
            })
        else:
            # Fallback: generate a clean branch name
            clean_name = folder_name.capitalize()
            if slug != 'assortment':
                clean_name += ' ' + slug.replace('-', ' ').title()
                
            branches_info.append({
                'slug': slug,
                'file_path': af,
                'name': clean_name,
                'address': 'Tirana',
                'location': [19.818, 41.320]
            })

    # IDs
    RESTAURANT_ID = rest_seq + 1
    USER_ID_START = user_seq + 1
    BRANCH_ID_START = loc_seq + 1
    CATEGORY_ID_START = cat_seq + 1
    OPTION_GROUP_ID_START = og_seq + 1
    OPTION_VARIANT_ID_START = ov_seq + 1
    MENU_ID_START = menu_seq + 1
    BRANCH_MENU_ITEM_ID_START = bmi_seq + 1

    PASS_HASH = "$2a$10$CG2pAzSgv1tPmmSW0A9IIuWKK5uhQ2iosNMp0bjgBDwYfSKPUQfCG"

    sql_lines = []
    sql_lines.append(f"-- {restaurant_name.upper()} --")
    sql_lines.append("\n-- USERS --")
    
    rest_manager_id = USER_ID_START
    sql_lines.append(f"INSERT INTO users (id, address, created_at, created_by_company, deliver_location_id, email, is_active, last_selected_payment_method_id, name, password, phone_number, profile_url, require_password_change, token_version, updated_at, verification_status) VALUES ({rest_manager_id}, 'Tirana', CURRENT_TIMESTAMP, NULL, NULL, 'manager@{restaurant_name.lower()}.al', true, NULL, '{restaurant_name} Manager', '{PASS_HASH}', '+355 69 000 00{rest_manager_id}', 'https://d3u269mlo8clta.cloudfront.net/user-profile-image/{rest_manager_id}.png', false, 1, CURRENT_TIMESTAMP, 'APPROVED');")
    sql_lines.append(f"INSERT INTO users_roles (user_id, role_id) VALUES ({rest_manager_id}, 3);")

    current_user_id = USER_ID_START + 1
    branch_managers = {}

    for b in branches_info:
        # Use restaurant_name for branch manager naming
        name = restaurant_name
        b_manager_id = current_user_id
        current_user_id += 1
        branch_managers[b['name']] = b_manager_id
        
        # Clean up the slug to generate a nice email (e.g. burger-king-astir -> astir@burgerking.al)
        # We split by '-' and take the last parts, or just use the whole slug if it's short
        slug_parts = b['slug'].split('-')
        branch_email_prefix = slug_parts[-1] if len(slug_parts) > 1 else b['slug']
        
        email = f"{branch_email_prefix}@{restaurant_name.lower()}.al"
        sql_lines.append(f"INSERT INTO users (id, address, created_at, created_by_company, deliver_location_id, email, is_active, last_selected_payment_method_id, name, password, phone_number, profile_url, require_password_change, token_version, updated_at, verification_status) VALUES ({b_manager_id}, 'Tirana', CURRENT_TIMESTAMP, {RESTAURANT_ID}, NULL, '{email}', true, NULL, '{restaurant_name} BranchManager', '{PASS_HASH}', '+355 69 000 00{current_user_id}', 'https://d3u269mlo8clta.cloudfront.net/user-profile-image/{b_manager_id}.png', false, 1, CURRENT_TIMESTAMP, 'APPROVED');")
        sql_lines.append(f"INSERT INTO users_roles (user_id, role_id) VALUES ({b_manager_id}, 4);")

    sql_lines.append("\n-- RESTAURANT --")
    # Set description, with special case for Laguna
    description = description_param if description_param else f"{restaurant_name} description"
    sql_lines.append(f"INSERT INTO restaurant (id, name, cover_image_url, profile_image_url, is_deleted, is_promoted, phone_number, created_at, user_id, description) VALUES ({RESTAURANT_ID}, '{restaurant_name}', 'https://d3u269mlo8clta.cloudfront.net/{restaurant_name.lower()}/cover.png', 'https://d3u269mlo8clta.cloudfront.net/{restaurant_name.lower()}/profile.png', false, true, '+355 69 111 6666', CURRENT_TIMESTAMP, {rest_manager_id}, '{description}') ON CONFLICT DO NOTHING;")
    sql_lines.append(f"INSERT INTO restaurant_has_categories (restaurant_id, category_id) VALUES ({RESTAURANT_ID}, 4);")

    sql_lines.append("\n-- RESTAURANT LOCATIONS & HOURS & PAYMENTS --")
    # Determine delivery radius: 8km if only one branch, else default 5km
    delivery_radius = 8.0 if len(branches_info) == 1 else 5.0
    current_branch_id = BRANCH_ID_START
    branch_id_map = {} 

    for b in branches_info:
        name = b['name']
        slug = b['slug']
        address = str(b['address']).replace("'", "''")
        loc = b['location'] 
        manager_id = branch_managers[name]
        b_id = current_branch_id
        branch_id_map[slug] = b_id
        current_branch_id += 1
        
        sql_lines.append(f"INSERT INTO restaurant_locations (id, address, location, phone_number, is_active, restaurant_id, delivery_radius_in_km, is_closed, min_order_amount, avg_prep_time_in_minutes, created_at, average_rating, review_count, deleted, daily_order_count, manager_id) VALUES ({b_id}, '{address}', ST_GeomFromText('POINT({loc[0]} {loc[1]})', 4326), '+355 69 000 {b_id}', true, {RESTAURANT_ID}, {delivery_radius}, false, 500, 20, CURRENT_TIMESTAMP, 0, 0, false, 0, {manager_id}) ON CONFLICT DO NOTHING;")
        
        days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
        for day in days:
            sql_lines.append(f"INSERT INTO opening_hours (restaurant_branch_id, close_time, day_of_week, open_time) VALUES ({b_id}, '23:00:00', '{day}', '09:00:00');")
        
        sql_lines.append(f"INSERT INTO restaurant_branch_payment_methods (restaurant_branch_id, \"payment_   method_id\") VALUES ({b_id}, 1);")
        sql_lines.append(f"INSERT INTO restaurant_branch_payment_methods (restaurant_branch_id, \"payment_   method_id\") VALUES ({b_id}, 3);")

    all_categories = {}
    all_items = {}
    all_options = {}

    current_cat_id = CATEGORY_ID_START
    current_og_id = OPTION_GROUP_ID_START
    current_ov_id = OPTION_VARIANT_ID_START
    current_menu_id = MENU_ID_START
    current_bmi_id = BRANCH_MENU_ITEM_ID_START

    branch_item_ids = {}

    for b in branches_info:
        slug = b['slug']
        with open(b['file_path'], 'r', encoding='utf-8') as f:
            assort = json.load(f)
        
        for cat in assort.get('categories', []):
            cat_name = cat['name']
            if cat_name not in all_categories:
                all_categories[cat_name] = current_cat_id
                current_cat_id += 1
                
        b_items = []
        for item in assort.get('items', []):
            w_id = item['id']
            b_items.append(w_id)
            if w_id not in all_items:
                all_items[w_id] = item
        branch_item_ids[slug] = b_items
        
        for opt in assort.get('options', []):
            o_id = opt['id']
            if o_id not in all_options:
                all_options[o_id] = opt

    sql_lines.append("\n-- CATEGORIES --")
    for c_name, c_id in all_categories.items():
        safe_name = c_name.replace("'", "''")
        sql_lines.append(f"INSERT INTO categories (id, name, deleted, restaurant_id) VALUES ({c_id}, '{safe_name}', false, {RESTAURANT_ID}) ON CONFLICT DO NOTHING;")

    sql_lines.append("\n-- OPTION GROUPS & VARIANTS --")
    og_map = {} 
    for o_id, opt in all_options.items():
        og_name = opt['name'].replace("'", "''")
        og_id = current_og_id
        og_map[o_id] = og_id
        current_og_id += 1
        
        sql_lines.append(f"INSERT INTO option_group (id, name, is_deleted, max_selection, min_selection, restaurant_id) VALUES ({og_id}, '{og_name}', false, 1, 0, {RESTAURANT_ID}) ON CONFLICT DO NOTHING;")
        
        for val in opt.get('values', []):
            ov_name = val['name'].replace("'", "''")
            ov_price = val.get('price', 0) // 100
            sql_lines.append(f"INSERT INTO option_variant (id, name, recommended_price, is_deleted, group_id) VALUES ({current_ov_id}, '{ov_name}', {ov_price}, false, {og_id}) ON CONFLICT DO NOTHING;")
            current_ov_id += 1

    sql_lines.append("\n-- MENUS & BRANCH MENU ITEMS & OPTIONS --")
    menu_id_map = {} 
    urls_api = []
    aws_base = f'https://d3u269mlo8clta.cloudfront.net/{restaurant_name.lower()}/'

    item_cat_map = {}
    for b in branches_info:
        slug = b['slug']
        with open(b['file_path'], 'r', encoding='utf-8') as f:
            assort = json.load(f)
            for cat in assort.get('categories', []):
                cat_db_id = all_categories[cat['name']]
                for i_id in cat.get('item_ids', []):
                    if i_id not in item_cat_map:
                        item_cat_map[i_id] = cat_db_id

    for w_id, item in all_items.items():
        name = item['name'].replace("'", "''")
        desc = item.get('description', '').replace("'", "''")
        
        # safely handle empty prices, use recommended_price or 0 if missing
        price_val = item.get('price', 0)
        if not price_val:
            price_val = 0
        price = price_val // 100
        
        cat_db_id = item_cat_map.get(w_id, list(all_categories.values())[0] if all_categories else 1)
            
        image_url = ''
        if item.get('images') and len(item['images']) > 0:
            api_url = item['images'][0]['url']
            img_name = api_url.split('/')[-1] + '.png'
            image_url = aws_base + img_name
            urls_api.append(f"{restaurant_name.lower()} {img_name} {api_url}")
            
        m_id = current_menu_id
        menu_id_map[w_id] = m_id
        current_menu_id += 1
        
        sql_lines.append(f"INSERT INTO menus (id, name, description, image_url, category_id) VALUES ({m_id}, '{name}', '{desc}', '{image_url}', {cat_db_id}) ON CONFLICT DO NOTHING;")
        
        for opt_ref in item.get('options', []):
            opt_id = opt_ref.get('option_id', opt_ref.get('id'))
            if opt_id in og_map:
                og_db_id = og_map[opt_id]
                sql_lines.append(f"INSERT INTO menu_option_groups (menu_id, group_id) VALUES ({m_id}, {og_db_id}) ON CONFLICT DO NOTHING;")

    sql_lines.append("\n-- ASSIGN BRANCH MENU ITEMS --")
    branch_bmis = {}
    for b in branches_info:
        slug = b['slug']
        b_id = branch_id_map[slug]
        branch_bmis[b_id] = []
        
        for w_id in branch_item_ids[slug]:
            if w_id in menu_id_map:
                m_id = menu_id_map[w_id]
                item = all_items[w_id]
                
                price_val = item.get('price', 0)
                if not price_val:
                    price_val = 0
                price = price_val // 100
                
                sql_lines.append(f"INSERT INTO branch_menu_items (id, branch_id, menu_id, price, is_highlighted, is_available) VALUES ({current_bmi_id}, {b_id}, {m_id}, {price}, false, true) ON CONFLICT DO NOTHING;")
                branch_bmis[b_id].append(current_bmi_id)
                current_bmi_id += 1

    sql_lines.append("\n-- HIGHLIGHTS --")
    for b_id, bmis in branch_bmis.items():
        if bmis:
            chosen = random.sample(bmis, min(4, len(bmis)))
            for c in chosen:
                sql_lines.append(f"UPDATE branch_menu_items SET is_highlighted = true WHERE id = {c};")

    sql_lines.append("\n-- UPDATE SEQUENCES --")
    sql_lines.append(f"SELECT setval('users_id_seq', {current_user_id - 1});")
    sql_lines.append(f"SELECT setval('restaurant_id_seq', {RESTAURANT_ID});")
    sql_lines.append(f"SELECT setval('restaurant_locations_id_seq', {current_branch_id - 1});")
    sql_lines.append(f"SELECT setval('categories_id_seq', {current_cat_id - 1});")
    sql_lines.append(f"SELECT setval('option_group_id_seq', {current_og_id - 1});")
    sql_lines.append(f"SELECT setval('option_variant_id_seq', {current_ov_id - 1});")
    sql_lines.append(f"SELECT setval('menus_id_seq', {current_menu_id - 1});")
    sql_lines.append(f"SELECT setval('branch_menu_items_id_seq', {current_bmi_id - 1});")

    output_sql = os.path.join(folder_path, f"{restaurant_name.lower()}_populating_db.sql")
    with open(output_sql, 'w', encoding='utf-8') as f:
        f.write('\n'.join(sql_lines))

    # for u_id in range(USER_ID_START, current_user_id):
    #     r_id = random.randint(1, 70)
    #     urls_api.insert(0, f"user-profile-image {u_id} https://xsgames.co/randomusers/assets/avatars/male/{r_id}.jpg")

    api_path = os.path.join(folder_path, "to_upload.api")
    with open(api_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(urls_api) + '\n')

    print(f"Done processing {folder_name}")
    print(f"SQL written to {output_sql}")
    print(f"API written to {api_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('folder_path')
    parser.add_argument('--users', type=int, required=True)
    parser.add_argument('--rest', type=int, required=True, help='Current restaurant_id_seq')
    parser.add_argument('--desc', type=str, default='', help='Description for the restaurant (optional)')
    parser.add_argument('--loc', type=int, required=True)
    parser.add_argument('--cat', type=int, required=True)
    parser.add_argument('--og', type=int, required=True)
    parser.add_argument('--ov', type=int, required=True)
    parser.add_argument('--menu', type=int, required=True)
    parser.add_argument('--bmi', type=int, required=True)
    args = parser.parse_args()

    process_restaurant(args.folder_path, args.users, args.rest, args.loc, args.cat, args.og, args.ov, args.menu, args.bmi, args.desc)
