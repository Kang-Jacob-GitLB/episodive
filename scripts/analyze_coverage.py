#!/usr/bin/env python3
"""
Jacoco Coverage Report Analyzer
Analyzes Jacoco XML reports and generates a markdown summary
"""

import xml.etree.ElementTree as ET
import os
import sys
from pathlib import Path
from datetime import datetime


def parse_coverage(xml_path):
    """Parse a Jacoco XML file and extract coverage data"""
    if not os.path.exists(xml_path):
        return None

    tree = ET.parse(xml_path)
    root = tree.getroot()

    module_data = {
        'name': root.get('name', 'Unknown'),
        'packages': [],
        'total_lines_missed': 0,
        'total_lines_covered': 0,
        'total_branches_missed': 0,
        'total_branches_covered': 0,
    }

    # Get overall counters
    for counter in root.findall("./counter"):
        counter_type = counter.get('type')
        missed = int(counter.get('missed', 0))
        covered = int(counter.get('covered', 0))

        if counter_type == 'LINE':
            module_data['total_lines_missed'] = missed
            module_data['total_lines_covered'] = covered
        elif counter_type == 'BRANCH':
            module_data['total_branches_missed'] = missed
            module_data['total_branches_covered'] = covered

    # Parse packages and classes
    for package in root.findall('.//package'):
        pkg_name = package.get('name', '').replace('/', '.')

        # Skip if package name contains excluded patterns
        if any(excl in pkg_name for excl in ['hilt_aggregated_deps', 'Hilt_']):
            continue

        pkg_data = {
            'name': pkg_name,
            'classes': []
        }

        for cls in package.findall('.//class'):
            class_name = cls.get('name', '').split('/')[-1]

            # Skip excluded classes
            excluded_patterns = [
                '$', 'Hilt_', '_Factory', '_Generated',
                '_MembersInjector', '_HiltModules', '_Provide',
                'Dagger', 'BuildConfig', 'ComposableSingletons'
            ]
            if any(excl in class_name for excl in excluded_patterns):
                continue

            class_data = {
                'name': class_name,
                'source_file': cls.get('sourcefilename', ''),
                'lines_missed': 0,
                'lines_covered': 0,
                'branches_missed': 0,
                'branches_covered': 0,
            }

            for counter in cls.findall("./counter"):
                counter_type = counter.get('type')
                missed = int(counter.get('missed', 0))
                covered = int(counter.get('covered', 0))

                if counter_type == 'LINE':
                    class_data['lines_missed'] = missed
                    class_data['lines_covered'] = covered
                elif counter_type == 'BRANCH':
                    class_data['branches_missed'] = missed
                    class_data['branches_covered'] = covered

            # Only add classes with actual code
            if class_data['lines_covered'] + class_data['lines_missed'] > 0:
                pkg_data['classes'].append(class_data)

        if pkg_data['classes']:
            module_data['packages'].append(pkg_data)

    return module_data


def calculate_percentage(covered, total):
    """Calculate coverage percentage"""
    if total == 0:
        return 100.0
    return (covered / total) * 100


def generate_markdown_report(modules_data, output_file):
    """Generate a markdown report from coverage data"""

    with open(output_file, 'w', encoding='utf-8') as f:
        # Header
        f.write("# Code Coverage Report\n\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("---\n\n")

        # Overall Summary
        f.write("## Overall Summary\n\n")
        f.write("| Module | Line Coverage | Branch Coverage | Total Lines | Covered | Missed |\n")
        f.write("|--------|--------------|----------------|-------------|---------|--------|\n")

        total_all_lines_covered = 0
        total_all_lines_missed = 0
        total_all_branches_covered = 0
        total_all_branches_missed = 0

        for module_name, data in sorted(modules_data.items()):
            if data is None:
                continue

            total_lines = data['total_lines_covered'] + data['total_lines_missed']
            total_branches = data['total_branches_covered'] + data['total_branches_missed']

            line_pct = calculate_percentage(data['total_lines_covered'], total_lines)
            branch_pct = calculate_percentage(data['total_branches_covered'], total_branches)

            total_all_lines_covered += data['total_lines_covered']
            total_all_lines_missed += data['total_lines_missed']
            total_all_branches_covered += data['total_branches_covered']
            total_all_branches_missed += data['total_branches_missed']

            f.write(f"| {module_name:15} | {line_pct:5.1f}% | {branch_pct:5.1f}% | "
                   f"{total_lines:6} | {data['total_lines_covered']:6} | {data['total_lines_missed']:6} |\n")

        # Project total
        project_total_lines = total_all_lines_covered + total_all_lines_missed
        project_total_branches = total_all_branches_covered + total_all_branches_missed
        project_line_pct = calculate_percentage(total_all_lines_covered, project_total_lines)
        project_branch_pct = calculate_percentage(total_all_branches_covered, project_total_branches)

        f.write("|--------|--------------|----------------|-------------|---------|--------|\n")
        f.write(f"| **TOTAL** | **{project_line_pct:5.1f}%** | **{project_branch_pct:5.1f}%** | "
               f"**{project_total_lines:6}** | **{total_all_lines_covered:6}** | **{total_all_lines_missed:6}** |\n\n")

        # Low Coverage Classes
        f.write("---\n\n")
        f.write("## Classes with Low Coverage (< 80%)\n\n")

        low_coverage_classes = []

        for module_name, data in modules_data.items():
            if data is None:
                continue

            for package in data['packages']:
                for cls in package['classes']:
                    total_lines = cls['lines_covered'] + cls['lines_missed']
                    if total_lines > 0:
                        pct = calculate_percentage(cls['lines_covered'], total_lines)
                        if pct < 80:
                            low_coverage_classes.append({
                                'module': module_name,
                                'package': package['name'],
                                'class': cls['name'] + '.kt',
                                'coverage': pct,
                                'covered': cls['lines_covered'],
                                'missed': cls['lines_missed'],
                                'total': total_lines
                            })

        # Sort by coverage percentage
        low_coverage_classes.sort(key=lambda x: x['coverage'])

        if low_coverage_classes:
            f.write("| Module | Package | Class | Coverage | Covered/Total | Missed |\n")
            f.write("|--------|---------|-------|----------|---------------|--------|\n")

            for cls in low_coverage_classes:
                f.write(f"| {cls['module']:10} | {cls['package'][:30]:30} | {cls['class'][:30]:30} | "
                       f"{cls['coverage']:5.1f}% | {cls['covered']}/{cls['total']} | {cls['missed']} |\n")
        else:
            f.write("✅ All classes have >= 80% coverage!\n")

        f.write("\n---\n\n")

        # Detailed Module Reports
        f.write("## Detailed Module Reports\n\n")

        for module_name, data in sorted(modules_data.items()):
            if data is None:
                continue

            total_lines = data['total_lines_covered'] + data['total_lines_missed']
            line_pct = calculate_percentage(data['total_lines_covered'], total_lines)

            f.write(f"### {module_name} - {line_pct:.1f}% Coverage\n\n")

            # Sort packages by coverage
            packages_with_coverage = []
            for package in data['packages']:
                pkg_lines_covered = sum(c['lines_covered'] for c in package['classes'])
                pkg_lines_missed = sum(c['lines_missed'] for c in package['classes'])
                pkg_total = pkg_lines_covered + pkg_lines_missed
                pkg_pct = calculate_percentage(pkg_lines_covered, pkg_total)
                packages_with_coverage.append((package, pkg_pct, pkg_lines_covered, pkg_total))

            packages_with_coverage.sort(key=lambda x: x[1])

            for package, pkg_pct, pkg_covered, pkg_total in packages_with_coverage:
                f.write(f"#### 📦 {package['name']} - {pkg_pct:.1f}%\n\n")
                f.write("| Class | Coverage | Lines Covered/Total | Branches Covered/Total |\n")
                f.write("|-------|----------|---------------------|------------------------|\n")

                # Sort classes by coverage
                classes_sorted = sorted(package['classes'],
                                       key=lambda c: calculate_percentage(c['lines_covered'],
                                                                         c['lines_covered'] + c['lines_missed']))

                for cls in classes_sorted:
                    total_lines = cls['lines_covered'] + cls['lines_missed']
                    total_branches = cls['branches_covered'] + cls['branches_missed']
                    line_pct = calculate_percentage(cls['lines_covered'], total_lines)

                    # Emoji indicators
                    if line_pct >= 90:
                        indicator = "✅"
                    elif line_pct >= 80:
                        indicator = "⚠️"
                    else:
                        indicator = "❌"

                    branches_str = f"{cls['branches_covered']}/{total_branches}" if total_branches > 0 else "N/A"
                    class_name_with_ext = cls['name'] + '.kt'

                    f.write(f"| {indicator} {class_name_with_ext[:40]:40} | {line_pct:5.1f}% | "
                           f"{cls['lines_covered']}/{total_lines} | {branches_str} |\n")

                f.write("\n")

        # Recommendations
        f.write("---\n\n")
        f.write("## Recommendations\n\n")

        if low_coverage_classes:
            zero_coverage = [c for c in low_coverage_classes if c['coverage'] == 0]
            if zero_coverage:
                f.write("### 🔴 Priority: Zero Coverage Classes\n\n")
                f.write("These classes have no test coverage and should be prioritized:\n\n")
                for cls in zero_coverage[:10]:
                    f.write(f"- **{cls['module']}/{cls['class']}** ({cls['total']} lines)\n")
                f.write("\n")

            low_but_not_zero = [c for c in low_coverage_classes if 0 < c['coverage'] < 50]
            if low_but_not_zero:
                f.write("### 🟡 Medium Priority: Low Coverage Classes (< 50%)\n\n")
                for cls in low_but_not_zero[:10]:
                    f.write(f"- **{cls['module']}/{cls['class']}** - {cls['coverage']:.1f}% "
                           f"({cls['missed']} lines missed)\n")
                f.write("\n")
        else:
            f.write("🎉 Excellent! All classes have good test coverage (>= 80%)\n\n")


def main():
    """Main entry point"""
    # Find all coverage reports
    project_root = Path(__file__).parent.parent

    print("Searching for coverage reports...")

    modules_data = {}

    # Core modules
    for module in ['database', 'network', 'datastore', 'data', 'domain', 'player', 'designsystem', 'testing']:
        xml_path = project_root / f'core/{module}/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml'
        if xml_path.exists():
            print(f"  [OK] Found coverage for core:{module}")
            modules_data[f'core:{module}'] = parse_coverage(str(xml_path))

    # Feature modules
    for module in ['home', 'search', 'library', 'podcast', 'player', 'clip', 'onboarding', 'channel']:
        xml_path = project_root / f'feature/{module}/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml'
        if xml_path.exists():
            print(f"  [OK] Found coverage for feature:{module}")
            modules_data[f'feature:{module}'] = parse_coverage(str(xml_path))

    # App module
    app_xml_path = project_root / 'app/build/reports/jacoco/createDebugCombinedCoverageReport/createDebugCombinedCoverageReport.xml'
    if app_xml_path.exists():
        print(f"  [OK] Found coverage for app")
        modules_data['app'] = parse_coverage(str(app_xml_path))

    if not modules_data:
        print("[ERROR] No coverage reports found!")
        print("   Run './gradlew createDebugCombinedCoverageReport' first")
        sys.exit(1)

    # Generate report
    docs_dir = project_root / 'docs'
    docs_dir.mkdir(exist_ok=True)
    output_file = docs_dir / 'COVERAGE_REPORT.md'
    print(f"\nGenerating report...")
    generate_markdown_report(modules_data, str(output_file))

    print(f"[OK] Coverage report generated: {output_file}")
    print(f"\nYou can view it with: cat {output_file}")


if __name__ == '__main__':
    main()