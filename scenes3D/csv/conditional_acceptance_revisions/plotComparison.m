clear 
close all

% Put name of scene name having 2 csv files in same directory as this file
%(one with name "<scene_name>.csv" and the other "<scene_name>_merged.csv")
scene_name = "funnel"


fontSize = 12;
fontName = 'Times New Roman';

smoothing = 3
plot_name = "Performance With Time for " + scene_name + " Scene"
X_unmerged = readtable(scene_name + ".csv");
X_merged = readtable(scene_name + "_merged_bsh.csv");

num_timesteps = min(height(X_unmerged), height(X_merged));

hfig = figure('Renderer', 'painters', 'Position', [0 0 900 700]), set(gcf,'color','w'); hold on;


hold;

h = zeros(4,1);


temp_m = movmean(X_merged{1:num_timesteps, 13} - X_merged{1:num_timesteps, 3}, smoothing);
temp_um = X_unmerged{1:num_timesteps, 13 } - X_unmerged{1:num_timesteps, 3};

subplot(2,1,1);
hold on;

h(1) = plot(movmean(temp_um, smoothing), 'DisplayName','Contact Resolution with Sleeping')

h(3) = plot(movmean(temp_m, smoothing), 'color',[0.8500 0.3250 0.0980], 'DisplayName','Contact Resolution with Merging')

h(2) =plot(movmean(X_unmerged{1:num_timesteps, 3}, smoothing),'color',[0.4940 0.1840 0.5560], 'DisplayName','Col. Detection with Sleeping')



h(4) = plot(movmean(X_merged{1:num_timesteps, 3}, smoothing), 'color',[0.9290 0.6940 0.1250], 'DisplayName','Col. Detection with Merging')


hold;

ylabel("Computation Time (s)")
xlabel("Time Step")
set(gca,'yscale','log')
set(gca, 'YTick', [10.^-4 10^-3 10.^-2 10^-1 10^0 ])
%xlim([0 50000])
ylim([0.000001 1])

set(gca, 'fontsize', fontSize, 'fontname', fontName);
lgd = legend(h, 'Location', 'southeast')
%lgd.FontSize=8
hold off

subplot(2,1,2);



hold on



plot(X_unmerged{1:num_timesteps, 2}, 'DisplayName','Contacts with Sleeping')

plot(X_unmerged{1:num_timesteps, 1}, 'color',[0.4940 0.1840 0.5560],  'DisplayName','Bodies with Sleeping')

plot(movmean(X_merged{1:num_timesteps, 2}, smoothing),  'color',[0.8500 0.3250 0.0980], 'DisplayName','Contacts with Merging')

plot(movmean(X_merged{1:num_timesteps, 1}, smoothing), 'color',[0.9290 0.6940 0.1250], 'DisplayName','Bodies with Merging')




set(gca,'yscale','log')
set(gca, 'YTick', [10.^0 10^1 10.^2 10^3 10^4 ])


lgd = legend('Location', 'southeast')
%lgd.FontSize = 8
%xlim([0 50000])
ylabel("Number")
xlabel("Time Step")



set(gca, 'fontsize', fontSize, 'fontname', fontName);
set(hfig,'Units','Inches');
pos = get(hfig,'Position');
set(hfig,'PaperPositionMode','Auto','PaperUnits','Inches','PaperSize',[pos(3), pos(4)])
print(hfig, scene_name + "_comparison",'-dpdf','-r0')