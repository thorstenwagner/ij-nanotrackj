// This script simulates brownian motion of particles by specifing their size. 
// It is possible to simulate polydisperse suspensions.
// The steplength are drawn from the probability density function of the real displacements
// as specified in formula (9) in 
//
// Michalet, X., 2010. Mean square displacement analysis of 
// single-particle trajectories with localization error: Brownian motion in an isotropic 
// medium. Physical Review E, 82(4), p.041914.
//
// Author: Thorsten Wagner (wagner at biomedical-imaging dot de)

/*
 * GUI - Start
 */
Dialog.create("NanoSim");
Dialog.addNumber("Number of Frames:", 100);
Dialog.addNumber("Image Width:",800);
Dialog.addNumber("Image Hight:",600);
Dialog.addNumber("Framerate [1/s]:", 30);
Dialog.addNumber("Temperature [C°]:", 22);
Dialog.addNumber("Particlequalities", 2);
Dialog.addSlider("Relative Drift", 0, 1.00001, 0);
Dialog.addNumber("Pixelsize [nm]", 166);

Dialog.addNumber("Seed", floor(random*1000));
Dialog.show();
slices = Dialog.getNumber();
iwidth = Dialog.getNumber();
iheight = Dialog.getNumber();
framerate =  Dialog.getNumber();
tempInC = Dialog.getNumber();
temp = tempInC + 273.15; 				//Kelvin
anzPartQuality = Dialog.getNumber();
driftfactor = Dialog.getNumber();
pxsize = Dialog.getNumber()*pow(10,-9); //m
seed = Dialog.getNumber();
kB = 1.3806488*pow(10,-23);				// [kg*m^2*K^-1*s^-2] (Andrade equation)
visk = exp(-6.944+2036.8/temp)*pow(10,-3) ;		// [Pa*s]
random("Seed",seed);

/*
 * GUI - Particle Qualities
 */
anzPart = newArray(anzPartQuality); //Number of particle of the specific particle quality
partDia = newArray(anzPartQuality); //Diameter of the particle
partDC = newArray(anzPartQuality); // Diffusion coefficients of the particles
sumAnzPart = 0;

for(i = 0; i < anzPartQuality; i++){
  Dialog.create("Particle Quality " + i);
  Dialog.addNumber("Diameter [nm]:", 100);
  Dialog.addNumber("Number of particles:",20);
  Dialog.show();
  partDia[i] = Dialog.getNumber()*pow(10,-9);
  anzPart[i] = Dialog.getNumber();
  partDC[i] = (kB*temp/(partDia[i]*3*PI*visk));  
  sumAnzPart = sumAnzPart + anzPart[i];
}

/*
 * Setup drift
 */
driftDirection = random*2*PI;


/*
 * Create Image
 */
diaString =""+partDia[0]*pow(10,9)+"nm ("+toString(anzPart[0]/sumAnzPart, 1)+")";
for(i=1; i < anzPartQuality; i++){
	diaString = diaString + "_"+partDia[i]*pow(10,9)+"nm ("+toString(anzPart[i]/sumAnzPart, 1)+")";
}
run("Hyperstack...", "title=["+diaString+"nm_size_"+iwidth+"x"+iheight+"_N"+sumAnzPart+"_frames_"+slices+"_seed_"+seed+"_hz_"+framerate+"]  type=8-bit display=Grayscale width="+iwidth+" height="+iheight+" channels=1 slices="+slices+" frames=1");
Stack.setFrameRate(1/framerate)
setMetadata("Info", "Diameter " + diaString + ",Relative drift " +driftfactor+ ", Number of Particles " + sumAnzPart + ", Temperature " + tempInC + ", Pixelsize " + pxsize + ", Viscosity: " + visk + ", Seed: " + seed);

/*
 * Spot size 
 */
width = 5;		//px
height = 5;		//px

setBatchMode(true)
setForegroundColor(255, 255, 255);
for(k=0; k<anzPartQuality;k++){
	dc = partDC[k];
	print(partDC[k]);
	meanStepLength = sqrt(3.14159)/sqrt(framerate/dc) * 1/pxsize; 
	print(meanStepLength);
	drift = driftfactor*meanStepLength;
	dx=cos(driftDirection)*drift;
	dy=sin(driftDirection)*drift;
	
	for(j=0; j<anzPart[k];j++){
		x = iwidth*random;
		y = iheight*random;
		setSlice(1);
		fillOval(x, y, width, height);

		for(i = 2; i <= slices; i++){
		   setSlice(i);
		   u = random;
		   alpha = random*2*PI;								// Draw random direction
		   steplength = sqrt(-4*dc*(1/framerate)*log(1-u)); // Draw random steplength, see
		   steplength = steplength* 1/pxsize; 				// Convert it in pixel
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
}
setBatchMode(false)
