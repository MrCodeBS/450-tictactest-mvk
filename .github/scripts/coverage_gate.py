import csv
import sys
import subprocess
import os

JACOCO_CSV = "build/reports/jacoco/test/jacocoTestReport.csv"

def get_current_coverage():
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
    return (covered_instr / total * 100) if total > 0 else 0

def get_main_coverage():
    try:
        # Get the last line of the coverage history on origin/main
        subprocess.run(['git', 'fetch', 'origin', 'main'], capture_output=True, check=True)
        result = subprocess.run(
            ['git', 'show', 'origin/main:.github/coverage-history.csv'],
            capture_output=True, text=True, check=True
        )
        lines = result.stdout.strip().split('\n')
        if len(lines) > 1:
            last_line = lines[-1]
            return float(last_line.split(',')[1])
    except Exception as e:
        print(f"Warning: Could not read main coverage history: {e}")
    return 0.0

def main():
    current_cov = get_current_coverage()
    main_cov = get_main_coverage()

    diff = current_cov - main_cov
    
    passed = diff >= 0
    
    # Format the output string
    diff_str = f"+{diff:.1f}" if diff > 0 else f"{diff:.1f}"
    status = "PASS - Coverage Gate bestanden" if passed else "FAIL - Coverage Gate nicht bestanden"
    
    output = f"""Test Coverage
main:       {main_cov:.1f} %
PR:         {current_cov:.1f} %
Änderung:   {diff_str} Prozentpunkte
{status}"""

    print(output)
    
    # Write to step summary if available
    if 'GITHUB_STEP_SUMMARY' in os.environ:
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
            f.write("```text\n")
            f.write(output + "\n")
            f.write("```\n")

    # Write to outputs for PR comment
    if 'GITHUB_OUTPUT' in os.environ:
        with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
            f.write("gate_output<<EOF\n")
            f.write(output + "\n")
            f.write("EOF\n")

    if not passed:
        sys.exit(1)

if __name__ == "__main__":
    main()
