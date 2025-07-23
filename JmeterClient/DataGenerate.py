import csv
import random

rows = [chr(i) for i in range(ord('A'), ord('Z') + 1)]  # A to Z
unique_seats = []

for zone in range(1, 101):  # 1 to 100
    for row in rows:       # A to Z
        for col in range(1, 31):  # 1 to 30
            unique_seats.append((zone, row, col))

duplicate_ratio = 1.1
duplicate_count = int(len(unique_seats) * duplicate_ratio)
duplicates = random.choices(unique_seats, k=duplicate_count)

all_seats = unique_seats + duplicates
random.shuffle(all_seats)

with open("seats_with_duplicates.csv", "w", newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['zoneId', 'row', 'column'])
    for seat in all_seats:
        writer.writerow(seat)

print(
    f"CSV Generated: total {len(all_seats)} requests（include {duplicate_count} duplicate）")
