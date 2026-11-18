import csv
import sys
import subprocess
import os

JACOCO_CSV = "build/reports/jacoco/test/jacocoTestReport.csv"
MIN_BRANCH_COVERAGE = 90.0

def get_current_metrics():
    if not os.path.exists(JACOCO_CSV):
        print(f"Error: {JACOCO_CSV} not found.")
        sys.exit(1)
    missed_instr = 0
    covered_instr = 0
    missed_branch = 0
    covered_branch = 0
    with open(JACOCO_CSV, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            missed_instr += int(row['INSTRUCTION_MISSED'])
            covered_instr += int(row['INSTRUCTION_COVERED'])
            missed_branch += int(row['BRANCH_MISSED'])
            covered_branch += int(row['BRANCH_COVERED'])
    total_instr = missed_instr + covered_instr
    total_branch = missed_branch + covered_branch

    inst_pct = (covered_instr / total_instr * 100) if total_instr > 0 else 0.0
    branch_pct = (covered_branch / total_branch * 100) if total_branch > 0 else 100.0
    return inst_pct, branch_pct

def get_main_coverage():
    try:
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
    current_inst, current_branch = get_current_metrics()
    main_cov = get_main_coverage()

    diff = current_inst - main_cov
    diff_str = f"+{diff:.1f}" if diff > 0 else f"{diff:.1f}"

    branch_gate_passed = current_branch >= MIN_BRANCH_COVERAGE
    diff_gate_passed = diff >= 0 or current_inst >= MIN_BRANCH_COVERAGE

    overall_passed = branch_gate_passed and diff_gate_passed

    status = "PASS - 90% Branch Coverage & Quality Gate bestanden" if overall_passed else "FAIL - Quality Gate nicht bestanden"

    output = f"""Test & Quality Gate Report
============================================
Branch Coverage:      {current_branch:.1f} % (Soll: >= {MIN_BRANCH_COVERAGE:.1f} %) -> {'OK' if branch_gate_passed else 'FAILED'}
Instruction Coverage: {current_inst:.1f} % (main: {main_cov:.1f} %, Diff: {diff_str} pp)
Status:               {status}
============================================"""

    print(output)

    if 'GITHUB_STEP_SUMMARY' in os.environ:
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as f:
            f.write("```text\n")
            f.write(output + "\n")
            f.write("```\n")

    if 'GITHUB_OUTPUT' in os.environ:
        with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
            f.write("gate_output<<EOF\n")
            f.write(output + "\n")
            f.write("EOF\n")

    if not overall_passed:
        sys.exit(1)

if __name__ == "__main__":
    main()
