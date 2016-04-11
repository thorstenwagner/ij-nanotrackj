// This script simulates brownian motion of particles by specifing their diffusion coefficient.
// The steplength are drawn from the probability density function of the real displacements
// as specified in formula (9) in 
//
// Michalet, X., 2010. Mean square displacement analysis of 
// single-particle trajectories with localization error: Brownian motion in an isotropic 
// medium. Physical Review E, 82(4), p.041914.
//
// Author: Thorsten Wagner (wagner at biomedical-imaging dot de)

Dialog.create("NanoSim");
Dialog.addNumber("Number of Frames:", 100);
Dialog.addNumber("Framewidth:", 800);
Dialog.addNumber("Frameheight:", 600);
Dialog.addNumber("Framerate [1/s]:", 30);
Dialog.addNumber("Diffusion Coefficient [10^-10 cm^2/s]:", 600);
Dialog.addNumber("Number of Particels", 2);
Dialog.addSlider("Relative Drift", 0.0, 1.00001, 0.0);
Dialog.addNumber("Pixelsize [nm]", 166);
Dialog.show();
slices = Dialog.getNumber();
iwidth=Dialog.getNumber();
iheight=Dialog.getNumber();
framerate =  Dialog.getNumber();		// [s]
dc = Dialog.getNumber() * pow(10,-14); 		// [10^-10 m^2 /s]
anzpart = Dialog.getNumber();
driftfactor = Dialog.getNumber();	
pxsize = Dialog.getNumber() * pow(10,-9);	// [m]

run("Hyperstack...", "title=HyperStack type=8-bit display=Grayscale width="+iwidth+" height="+iheight+" channels=1 slices="+slices+" frames=1");
Stack.setFrameRate(1/framerate)
run("Invert", "stack");
meanStepLength = sqrt(3.14159)/sqrt(framerate/dc) * 1/pxsize; 
drift = driftfactor*meanStepLength;
driftDirection = random*2*PI;
dx=cos(driftDirection)*drift;
dy=sin(driftDirection)*drift;
width = 5;
height = 5;

setBatchMode(true)
setForegroundColor(0, 0, 0);
for(j=0; j<anzpart;j++){
	x = iwidth*random;
	y = iheight*random;
	setSlice(1);
	fillOval(x, y, width, height);

	for(i = 2; i <= slices; i++){
	   setSlice(i);
	   u = random;
	   alpha = random*2*PI;					// Draw random direction
	   steplength = sqrt(-4*dc*(1/framerate)*log(1-u)); 	// Draw random steplength, see
	   steplength = steplength* 1/pxsize; 			// Convert it in pixel
	   x=x+cos(alpha)*steplength + dx;
	   y=y+sin(alpha)*steplength + dy;		
	   if(x>iwidth){
	   	x = 0;
	   }
	   if(x<0){
	   	x = iwidth;
	   }
	   if(y>iheight){
	   	y = 0;
	   }
	   if(y<0){
	   	y = iheight;
	   }

	   
	   fillOval(x, y, width, height);
	}
}
setBatchMode(false)
