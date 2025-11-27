// Global chart references for real-time updates
let barChartInstance = null;
let donutChartInstance = null;

document.addEventListener('DOMContentLoaded', function () {
    fetchChartData();
});

async function fetchChartData() {
    try {
        const response = await fetch('/api/dashboard/stats');
        if (!response.ok) throw new Error('Failed to fetch chart data');
        const data = await response.json();
        renderCharts(data);
    } catch (error) {
        console.error('Error loading charts:', error);
    }
}

// Function to update charts with new data (called from WebSocket)
function updateChartsWithData(data) {
    console.log('[Charts] 📊 Atualizando gráficos com:', data);
    
    if (barChartInstance) {
        console.log('[Charts] Atualizando bar chart...');
        barChartInstance.updateSeries([{
            name: 'Tarefas',
            data: [data.todo, data.inProgress, data.done, data.cancelled]
        }]);
    } else {
        console.warn('[Charts] barChartInstance não disponível');
    }
    
    if (donutChartInstance) {
        console.log('[Charts] Atualizando donut chart...');
        donutChartInstance.updateSeries([data.todo, data.inProgress, data.done, data.cancelled]);
    } else {
        console.warn('[Charts] donutChartInstance não disponível');
    }
}

// Expose for WebSocket updates
window.updateChartsWithData = updateChartsWithData;

function renderCharts(data) {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    const themeMode = isDark ? 'dark' : 'light';
    const textColor = isDark ? '#e0e0e0' : '#333333';

    // --- Bar Chart: Tasks Overview ---
    const barOptions = {
        series: [{
            name: 'Tarefas',
            data: [data.todo, data.inProgress, data.done, data.cancelled]
        }],
        chart: {
            type: 'bar',
            height: 350,
            fontFamily: 'Outfit, sans-serif',
            background: 'transparent',
            toolbar: { show: false },
            animations: {
                enabled: true,
                easing: 'easeinout',
                speed: 800
            }
        },
        colors: ['#3b82f6', '#f59e0b', '#10b981', '#ef4444'],
        plotOptions: {
            bar: {
                borderRadius: 8,
                columnWidth: '50%',
                distributed: true,
            }
        },
        dataLabels: { enabled: false },
        legend: { show: false },
        xaxis: {
            categories: ['A Fazer', 'Em Progresso', 'Concluídas', 'Canceladas'],
            labels: {
                style: { colors: textColor, fontSize: '12px' }
            },
            axisBorder: { show: false },
            axisTicks: { show: false }
        },
        yaxis: {
            labels: {
                style: { colors: textColor }
            }
        },
        grid: {
            borderColor: isDark ? '#404040' : '#e5e7eb',
            strokeDashArray: 4,
        },
        theme: { mode: themeMode },
        tooltip: { theme: themeMode }
    };

    const barChart = new ApexCharts(document.querySelector("#tasksChart"), barOptions);
    barChart.render();
    barChartInstance = barChart; // Store reference for updates

    // --- Donut Chart: Status Distribution ---
    const donutOptions = {
        series: [data.todo, data.inProgress, data.done, data.cancelled],
        chart: {
            type: 'donut',
            height: 350,
            fontFamily: 'Outfit, sans-serif',
            background: 'transparent',
            animations: {
                enabled: true,
                easing: 'easeinout',
                speed: 800
            }
        },
        labels: ['A Fazer', 'Em Progresso', 'Concluídas', 'Canceladas'],
        colors: ['#3b82f6', '#f59e0b', '#10b981', '#ef4444'],
        plotOptions: {
            pie: {
                donut: {
                    size: '70%',
                    labels: {
                        show: true,
                        total: {
                            show: true,
                            label: 'Total',
                            color: textColor,
                            formatter: function (w) {
                                return w.globals.seriesTotals.reduce((a, b) => a + b, 0);
                            }
                        },
                        value: {
                            color: textColor,
                            fontSize: '24px',
                            fontWeight: 600
                        }
                    }
                }
            }
        },
        dataLabels: { enabled: false },
        legend: {
            position: 'bottom',
            labels: { colors: textColor }
        },
        stroke: { show: false },
        theme: { mode: themeMode },
        tooltip: { theme: themeMode }
    };

    const donutChart = new ApexCharts(document.querySelector("#statusDonutChart"), donutOptions);
    donutChart.render();
    donutChartInstance = donutChart; // Store reference for updates
    
    // Listen for theme changes to update charts
    const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
            if (mutation.attributeName === 'data-theme') {
                const newTheme = document.documentElement.getAttribute('data-theme');
                const newMode = newTheme === 'dark' ? 'dark' : 'light';
                const newTextColor = newTheme === 'dark' ? '#e0e0e0' : '#333333';
                const newGridColor = newTheme === 'dark' ? '#404040' : '#e5e7eb';
                
                barChart.updateOptions({
                    theme: { mode: newMode },
                    xaxis: { labels: { style: { colors: newTextColor } } },
                    yaxis: { labels: { style: { colors: newTextColor } } },
                    grid: { borderColor: newGridColor },
                    tooltip: { theme: newMode }
                });
                
                donutChart.updateOptions({
                    theme: { mode: newMode },
                    plotOptions: { pie: { donut: { labels: { total: { color: newTextColor }, value: { color: newTextColor } } } } },
                    legend: { labels: { colors: newTextColor } },
                    tooltip: { theme: newMode }
                });
            }
        });
    });
    
    observer.observe(document.documentElement, { attributes: true });
}
