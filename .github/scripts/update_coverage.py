import csv
import sys
import os
import datetime
import matplotlib.pyplot as plt

JACOCO_CSV = "build/reports/jacoco/test/jacocoTestReport.csv"
HISTORY_CSV = ".github/coverage-history.csv"
CHART_IMG = ".github/coverage-chart.png"

def main():
    if not os.path.exists(JACOCO_CSV):
        print(f"Error: {JACOCO_CSV} not found.")
        sys.exit(1)

    missed_instr = 0
    covered_instr = 0

    with open(JACOCO_CSV, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            missed_instr += int(row['INSTRUCTION_MISSED'])
            covered_instr += int(row['INSTRUCTION_COVERED'])

    total = missed_instr + covered_instr
    percentage = (covered_instr / total * 100) if total > 0 else 0
    percentage = round(percentage, 2)

    print(f"Current Coverage: {percentage}%")

    # Update history CSV
    history = []
    if os.path.exists(HISTORY_CSV):
        with open(HISTORY_CSV, 'r') as f:
            reader = csv.reader(f)
            history = list(reader)
    
    if not history or history[0] != ['Timestamp', 'Coverage']:
        history = [['Timestamp', 'Coverage']]

    now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
    history.append([now, str(percentage)])

    os.makedirs(os.path.dirname(HISTORY_CSV), exist_ok=True)
    with open(HISTORY_CSV, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(history)

    # Generate Chart
    dates = [row[0] for row in history[1:]]
    coverages = [float(row[1]) for row in history[1:]]

    plt.figure(figsize=(10, 5))
    plt.plot(dates, coverages, marker='o', linestyle='-', color='b')
    plt.title('JaCoCo Code Coverage Over Time')
    plt.xlabel('Timestamp (UTC)')
    plt.ylabel('Coverage (%)')
    plt.ylim(0, 105)
    
    # Hide x-axis labels if there are too many, or just keep it simple
    plt.xticks(rotation=45, ha='right')
    # If more than 10 runs, only show some ticks to avoid overlapping
    if len(dates) > 10:
        plt.gca().xaxis.set_major_locator(plt.MaxNLocator(10))

    plt.grid(True)
    plt.tight_layout()
    plt.savefig(CHART_IMG)
    print(f"Chart saved to {CHART_IMG}")

if __name__ == "__main__":
    main()
