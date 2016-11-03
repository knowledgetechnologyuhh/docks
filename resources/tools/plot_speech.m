% Parameters (metadata) the data was acquired with.
frame_length_ms = 30;
frame_shift_ms = 10;
frames_for_training = 400;

%input_filename = 'foo.wav';
%input_filename = 'trainset/noizeus_train/10dB/sp11_train_sn10.wav';
%input_filename = 'trainset/noizeus_train/clean/sp12.wav';
input_filename = 'trainset/sp12_train_sn10_preceeding_noise_sp12_train_sn10_again.wav';
%input_filename = 'trainset/sp12_train_sn10_preceeding_noise.wav';
%input_filename = 'trainset/noizeus_train/10db/sp13_train_sn10.wav';
%input_filename = 'trainset/noise_only.wav';
%input_filename = 'trainset/sp12_train_sn10_then_noise_only.wav';
%input_filename = 'trainset/noizeus_train/car_10dB/sp12_car_sn10.wav';


% Read file. Meta data can also be in the result file (first line).
% If the meta data is not in the file, the parameters specified above are used.
fid = fopen('/tmp/vqvad_classification_result');
metadata = fgetl(fid);

if feof(fid)
	% Simple mode, use meta data as specified above
	rawmask = metadata;
	metadata = '';
else
	rawmask = fgetl(fid);
end

eval(metadata);

frame_mask = [];
for x = rawmask
	if x == 'O'
		frame_mask = [frame_mask; 1];
	elseif x == '_'
		frame_mask = [frame_mask; 0];
	end
end

[s, fs] = wavread(input_filename);

frame_length = (frame_length_ms/1000) * fs;

% make s divisable by the frame length
s = s(1:length(s) - mod(length(s), frame_length));

% ignore potential padding frames at the end?
if floor(length(s) / frame_length) < length(frame_mask)
	frame_mask = frame_mask(1:floor(length(s) / frame_length));
end

signal_mask = [];
for i = 1:length(frame_mask)
	x = frame_mask(i);
	signal_mask = [signal_mask; repmat(x, frame_length, 1)];
end

% every 200*frame_length_ms training begins. Note however,
% that this counts on the frames that are generated using overlap.
% This means that for each full frame we have (frame_length_ms/frame_shift_ms)
% frames more, which in turn means that not every 200th frame training
% begins but every 200/(frame_length_ms/frame_shift_ms) frame.
training_events = [];
training_frame_count = floor(frames_for_training / (frame_length_ms / frame_shift_ms));
for i = 1:floor((length(s) / fs) / (frame_length_ms/1000*training_frame_count))
	training_events = [training_events; i * (frame_length_ms/1000) * training_frame_count];
end

figure(1); clf;

% Plot the results
%subplot(211);
hold all;
t = (0:length(s)-1)./fs;
plot(t, signal_mask * 0.5);
plot(t, s); xlabel('Time (sec)'); ylabel('Amplitude');
plot(training_events, zeros(length(training_events),1),'x');

for e = training_events'
	line([e,e], [-0.7,0.7]);
end

set(gca, 'XTick', min(t):0.200:max(t));
%set(gca, 'XTick', unique(floor(t)));
