"""Tables and figures of a measurement campaign.

usage: .venv/bin/python analyze.py ../results/<campaign-id>

The tables are printed, the figures are written to ../results/<campaign-id>/figures/.
"""
import json
import statistics
import sys
from datetime import datetime
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # files only, no window
import matplotlib.pyplot as plt
from matplotlib.ticker import MultipleLocator

INVARIANTS = ["NO_LOST_REQUEST", "NO_LOST_OUTCOME", "NO_DUPLICATE_CHARGE"]

PANEL_TITLES = {
    "S0_V0": "V0 (dual write)",
    "S0_V1": "V1 (outbox + CDC)",
    "S1_V0": "V0 (dual write)",
    "S1_V1": "V1 (outbox + CDC)",
    "S2_V1": "V1 (outbox + CDC)",
    "S3_V0": "V0 (dual write)",
    "S3_V1": "V1 (outbox + CDC)",
    "S4_V1": "V1 (outbox + CDC)",
    "S5_V1_WITHOUT_KEY": "V1, idempotencia-kulcs nélkül",
    "S5_V1_WITH_KEY": "V1, idempotencia-kulccsal",
    "S6_V1": "V1 (outbox + CDC)",
}

OUTCOMES = ["CREATED", "REJECTED", "NO_RESPONSE"]
OUTCOME_LABELS = {"CREATED": "201 Created", "REJECTED": "elutasítva (5xx)", "NO_RESPONSE": "nincs válasz"}
OUTCOME_COLORS = {"CREATED": "#2e7d32", "REJECTED": "#c62828", "NO_RESPONSE": "#ef6c00"}
PENDING_COLOR = "#1565c0"


# --- loading -------------------------------------------------------------------------------------

def load_campaign(directory):
    # the runs grouped by scenario, in the order of the campaign, e.g. {"S1_V1": [run 1, run 2, ...]}
    campaign = json.loads((directory / "campaign.json").read_text())
    runs = {scenario: [] for scenario in campaign["scenarios"]}
    for file in sorted(directory.glob("*.json")):
        if file.name == "campaign.json":
            continue
        run = json.loads(file.read_text())
        runs[run["metadata"]["scenario"]].append(run)
    for scenario_runs in runs.values():
        scenario_runs.sort(key=lambda run: run["metadata"]["repetition"])
    # a scenario whose every run failed has no result file, it is only in the failedRuns of campaign.json
    return campaign, {scenario: scenario_runs for scenario, scenario_runs in runs.items() if scenario_runs}


def parse(value):
    return datetime.fromisoformat(value) if value else None


# --- the values of a run for the tables ------------------------------------------------------------

def violation_total(run):
    return sum(run["invariants"][invariant]["violationCount"] for invariant in INVARIANTS)


def outage_millis(run):
    # all outages of the run together, None if there was none
    outages = run["timeline"]["outages"]
    return sum(outage["unavailableMillis"] for outage in outages) if outages else None


def availability_percent(run):
    # A: the share of the 201 responses among the requests sent during the outages
    outcomes = run["timeline"]["requestOutcomesDuringOutages"]
    total = sum(outcomes.values())
    return 100 * outcomes["CREATED"] / total if total else None


def summary(values):
    # median (min–max), the missing (None) values counted separately, e.g. "4270 (4100–4400), 1× nincs"
    present = [value for value in values if value is not None]
    missing = len(values) - len(present)
    if not present:
        return "–"
    text = f"{statistics.median(present):.0f}"
    if min(present) != max(present):
        text += f" ({min(present):.0f}–{max(present):.0f})"
    if missing:
        text += f", {missing}× nincs"
    return text


def verdict(runs, invariant):
    # "PASS" when every run passed, otherwise the failed runs and the number of violations, e.g. "FAIL 5/5: 1"
    results = [run["invariants"][invariant] for run in runs]
    failed = [result for result in results if result["verdict"] == "FAIL"]
    if not failed:
        return "PASS"
    return f"FAIL {len(failed)}/{len(runs)}: " + summary([result["violationCount"] for result in results])


# --- tables --------------------------------------------------------------------------------------

def print_table(title, header, rows):
    # every column as wide as its widest cell
    widths = [max(len(str(row[i])) for row in [header] + rows) for i in range(len(header))]
    print(title)
    print("  ".join(cell.ljust(width) for cell, width in zip(header, widths)))
    print("  ".join("-" * width for width in widths))
    for row in rows:
        print("  ".join(str(cell).ljust(width) for cell, width in zip(row, widths)))
    print()


def print_tables(runs):
    rows = []
    for scenario, scenario_runs in runs.items():
        thesis_ids = [scenario_runs[0]["invariants"][invariant]["thesisId"] for invariant in INVARIANTS]
        converged = sum(1 for run in scenario_runs if run["converged"])
        rows.append([scenario, len(scenario_runs), f"{converged}/{len(scenario_runs)}"]
                    + [verdict(scenario_runs, invariant) for invariant in INVARIANTS])
    print_table("1. Invariánsok (FAIL k/n: a sértések száma mediánnal és min–max értékkel)",
                ["konfiguráció", "n", "konvergált"] + thesis_ids, rows)

    rows = []
    for scenario, scenario_runs in runs.items():
        rows.append([
            scenario,
            summary([run["metrics"]["duplicateDeliveries"]["subscription-service"] for run in scenario_runs]),
            summary([run["metrics"]["duplicateDeliveries"]["payment-service"] for run in scenario_runs]),
            summary([run["metrics"]["duplicateRequests"] for run in scenario_runs]),
            summary([outage_millis(run) for run in scenario_runs]),
            summary([run["timeline"]["recoveryMillis"] for run in scenario_runs]),
            summary([availability_percent(run) for run in scenario_runs]),
        ])
    print_table("2. Hibatűrés (medián (min–max); nincs: nem volt kiesés, vagy nem állt helyre)",
                ["konfiguráció", "D subscription", "D payment", "dupla PSP-kérés", "kiesés [ms]", "T [ms]", "A [%]"],
                rows)

    rows = []
    for scenario, scenario_runs in runs.items():
        latencies = [run["metrics"]["latencyMillis"] for run in scenario_runs]
        rows.append([scenario] + [summary([latency[key] for latency in latencies]) for key in ["p50", "p95", "max"]])
    print_table("3. L: késleltetés a létrehozástól a lezárásig (medián (min–max) a futások között)",
                ["konfiguráció", "L p50 [ms]", "L p95 [ms]", "L max [ms]"], rows)


# --- the representative run ----------------------------------------------------------------------

def representative(runs):
    # the median run: by T when every run recovered, otherwise by the violations, then by the p50 latency
    if all(run["timeline"]["recoveryMillis"] is not None for run in runs):
        ordered = sorted(runs, key=lambda run: run["timeline"]["recoveryMillis"])
    else:
        ordered = sorted(runs, key=lambda run: (violation_total(run), run["metrics"]["latencyMillis"]["p50"]))
    return ordered[(len(ordered) - 1) // 2]


# --- the curves of a run -------------------------------------------------------------------------

class Panel:
    """The curves of one run in seconds from the start of the load."""

    def __init__(self, scenario, run):
        self.scenario = scenario
        self.run = run
        requests = run["rawData"]["requests"]
        self.zero = min(parse(request["sentAt"]) for request in requests)

        # only the subscriptions of the load, the warm-up ones were created before t = 0
        load_customers = {request["customerId"] for request in requests}
        self.subscriptions = [(self.seconds(subscription["createdAt"]), self.seconds(subscription["completedAt"]))
                              for subscription in run["rawData"]["subscriptions"]
                              if subscription["customerId"] in load_customers]
        # the time of the response, not of the sending
        self.answered = {outcome: [self.seconds(request["finishedAt"]) for request in requests
                                   if request["outcome"] == outcome]
                         for outcome in OUTCOMES}
        self.events = [(self.seconds(event["at"]), event["container"], event["action"])
                       for event in run["rawData"]["containerEvents"]]

        times = ([completed for _, completed in self.subscriptions if completed is not None]
                 + [t for answered in self.answered.values() for t in answered]
                 + [t for t, _, _ in self.events])
        self.end = max(times) + 3

    def seconds(self, value):
        return (parse(value) - self.zero).total_seconds() if value else None

    def pending(self, end):
        # exact steps: +1 when a subscription is created, -1 when it is completed
        changes = sorted([(created, 1) for created, _ in self.subscriptions]
                         + [(completed, -1) for _, completed in self.subscriptions if completed is not None])
        times, counts, count = [-1.0], [0], 0
        for t, change in changes:
            count += change
            times.append(t)
            counts.append(count)
        return times + [end], counts + [count]

    def cumulative(self, outcome, end):
        # exact steps: +1 at every response
        ordered = sorted(self.answered[outcome])
        return [-1.0] + ordered + [end], [0] + list(range(1, len(ordered) + 1)) + [len(ordered)]


# --- figures -------------------------------------------------------------------------------------

def mark_outages(ax, events, end):
    # a grey band from the die event until the healthy event, a dashed line where the container started again
    for died_at, container, action in events:
        if action != "die":
            continue
        healthy_at = next((t for t, c, a in events if c == container and a == "health_status: healthy" and t > died_at), end)
        started_at = next((t for t, c, a in events if c == container and a == "start" and t > died_at), None)
        ax.axvspan(died_at, healthy_at, color="#9e9e9e", alpha=0.25, label=f"{container} nem elérhető (die → healthy)")
        if started_at is not None:
            ax.axvline(started_at, color="#616161", linestyle="--", linewidth=1, label=f"{container} újraindul (start)")


def draw_figure(name, panels, directory):
    # one panel per configuration below each other, with the same time and count scale
    end = max(panel.end for panel in panels)
    outcomes = [outcome for outcome in OUTCOMES if any(panel.answered[outcome] for panel in panels)]

    # a larger font, because Word shrinks the figure to the page width
    with plt.rc_context({"font.size": 12}):
        fig, axes = plt.subplots(len(panels), 1, figsize=(10, 1.2 + 3.4 * len(panels)), sharex=True, sharey=True,
                                 squeeze=False)
        axes = [row[0] for row in axes]
        legend = {}
        for ax, panel in zip(axes, panels):
            for outcome in outcomes:
                ax.step(*panel.cumulative(outcome, end), where="post", color=OUTCOME_COLORS[outcome], linewidth=1.8,
                        label=f"{OUTCOME_LABELS[outcome]} (összesen)")
            ax.step(*panel.pending(end), where="post", color=PENDING_COLOR, linewidth=1.8, label="PENDING előfizetések")
            mark_outages(ax, panel.events, end)
            ax.set_xlim(-1, end)
            ax.set_title(PANEL_TITLES[panel.scenario])
            ax.set_ylabel("darab")
            ax.xaxis.set_major_locator(MultipleLocator(5))
            ax.xaxis.set_minor_locator(MultipleLocator(1))
            ax.grid(alpha=0.35, which="major")
            ax.grid(alpha=0.12, which="minor")
            # every label once, even if it is on more panels
            for handle, label in zip(*ax.get_legend_handles_labels()):
                legend.setdefault(label, handle)
        axes[-1].set_xlabel("idő a terhelés kezdetétől [s]")

        fig.tight_layout()
        # the legend below the panels, bbox_inches="tight" makes room for it
        fig.legend(list(legend.values()), list(legend.keys()), loc="upper center", bbox_to_anchor=(0.5, 0), ncol=3,
                   fontsize=10)
        file = directory / f"{name}.png"
        fig.savefig(file, dpi=200, bbox_inches="tight")
        plt.close(fig)
    return file


def draw_figures(runs, directory):
    # one figure per scenario, e.g. S3 with S3_V0 and S3_V1, S5 with the runs without and with the key
    directory.mkdir(exist_ok=True)
    groups = {}
    for scenario, scenario_runs in runs.items():
        groups.setdefault(scenario.split("_")[0], []).append((scenario, representative(scenario_runs)))

    print("Ábrák (a reprezentatív futás konfigurációnként)")
    for name, chosen in groups.items():
        file = draw_figure(name, [Panel(scenario, run) for scenario, run in chosen], directory)
        print(f"  {file.name}: " + ", ".join(run["metadata"]["id"] for _, run in chosen))


def main():
    directory = Path(sys.argv[1])
    campaign, runs = load_campaign(directory)
    print(f"Kampány {campaign['id']}: {campaign['runCount']} futás, ebből {len(campaign['failedRuns'])} sikertelen, "
          f"{campaign['loadCount']} kérés {campaign['loadRatePerSecond']}/s ütemben")
    for failed in campaign["failedRuns"]:
        print(f"  sikertelen: {failed['scenario']}, {failed['repetition']}. ismétlés: {failed['error']}")
    print()
    print_tables(runs)
    draw_figures(runs, directory / "figures")


main()
