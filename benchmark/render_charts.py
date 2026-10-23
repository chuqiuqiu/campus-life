import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
import matplotlib.font_manager as fm

plt.rcParams['font.family'] = 'Microsoft YaHei'
plt.rcParams['axes.unicode_minus'] = False

base = json.load(open('benchmark/results/baseline.json', encoding='utf-8'))['levels']
opt = json.load(open('benchmark/results/optimized.json', encoding='utf-8'))['levels']
x = [r['concurrency'] for r in base]
bq = [r['qps'] for r in base]
oq = [r['qps'] for r in opt]
bp95 = [r['p95_ms'] for r in base]
op95 = [r['p95_ms'] for r in opt]

INK = '#0F172A'
MUTED = '#64748B'
GRID = '#E2E8F0'
BASE = '#F97316'
OPT = '#4F46E5'
BG = '#FFFFFF'

fig, axes = plt.subplots(1, 2, figsize=(15.2, 6.1), dpi=200)
fig.patch.set_facecolor(BG)

for ax in axes:
    ax.set_facecolor(BG)
    ax.grid(True, axis='y', color=GRID, linewidth=1)
    ax.set_axisbelow(True)
    for spine in ['top', 'right']:
        ax.spines[spine].set_visible(False)
    for spine in ['left', 'bottom']:
        ax.spines[spine].set_color('#CBD5E1')
    ax.tick_params(colors=MUTED, labelsize=11)
    ax.set_xscale('log')
    ax.set_xticks(x)
    ax.set_xticklabels([str(v) for v in x])
    ax.minorticks_off()

# QPS panel
ax = axes[0]
ax.plot(x, bq, color=BASE, linewidth=2.6, marker='o', markersize=7, label='关闭缓存（基线）')
ax.plot(x, oq, color=OPT, linewidth=2.6, marker='o', markersize=7, label='开启三级缓存')
ax.fill_between(x, bq, oq, color=OPT, alpha=0.07)
ax.set_title('吞吐量：QPS 随并发数变化', fontsize=16, color=INK, pad=14, fontweight='bold')
ax.set_xlabel('并发数', fontsize=12, color=MUTED, labelpad=8)
ax.set_ylabel('QPS（requests / second）', fontsize=12, color=MUTED, labelpad=8)
ax.set_ylim(0, 1350)
ax.annotate('1,156 QPS', xy=(30, 1156), xytext=(14, 1195), fontsize=11, color=OPT,
            fontweight='bold', arrowprops=dict(arrowstyle='-', color=OPT, lw=1.2))
ax.annotate('359 QPS', xy=(60, 359), xytext=(58, 520), fontsize=11, color=BASE,
            fontweight='bold', arrowprops=dict(arrowstyle='-', color=BASE, lw=1.2))
ax.text(1.2, 1265, '并发 30 时提升 3.5×', fontsize=12, color=INK, fontweight='bold')
ax.legend(loc='lower right', frameon=False, fontsize=11)

# P95 panel
ax = axes[1]
ax.plot(x, bp95, color=BASE, linewidth=2.6, marker='s', markersize=7, label='关闭缓存（基线）')
ax.plot(x, op95, color=OPT, linewidth=2.6, marker='s', markersize=7, label='开启三级缓存')
ax.fill_between(x, bp95, op95, color=OPT, alpha=0.07)
ax.set_title('响应时间：P95 随并发数变化', fontsize=16, color=INK, pad=14, fontweight='bold')
ax.set_xlabel('并发数', fontsize=12, color=MUTED, labelpad=8)
ax.set_ylabel('P95 延迟（ms，越低越好）', fontsize=12, color=MUTED, labelpad=8)
ax.set_ylim(0, 600)
ax.annotate('32 ms', xy=(30, 32.31), xytext=(40, 105), fontsize=11, color=OPT,
            fontweight='bold', arrowprops=dict(arrowstyle='-', color=OPT, lw=1.2))
ax.annotate('167 ms', xy=(30, 166.73), xytext=(9, 235), fontsize=11, color=BASE,
            fontweight='bold', arrowprops=dict(arrowstyle='-', color=BASE, lw=1.2))
ax.text(1.2, 560, '并发 30 时 P95 降低 81%', fontsize=12, color=INK, fontweight='bold')

fig.text(0.5, -0.015,
         '测试接口：GET /items/page?pageNo=1&pageSize=8（直连 item-service，隔离网关与 Feign）   ·   20s/轮 × 2 轮取中位数   ·   预热 5s   ·   零错误率',
         ha='center', fontsize=9.5, color=MUTED)
fig.text(0.5, -0.055,
         '环境：AMD Ryzen 5 3550H（4C/8T）· 16GB · Windows 11 · JDK 17 · MySQL 9.7/Redis 6.2 运行于 VMware 虚拟机',
         ha='center', fontsize=9.5, color=MUTED)

fig.tight_layout(rect=(0, 0.02, 1, 1))
fig.savefig('docs/assets/benchmark/qps-latency.png', bbox_inches='tight', facecolor=BG)
print('saved charts')
