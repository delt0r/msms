#!R 
animateTrace<-function(x,frames){
	#first we need to get the traces...
	nans<-is.nan(x[1,])
	delims<-which(nans)
	limx<-c(min(x[1,!nans]),max(x[1,!nans]))
	limy<-c(min(x[2,!nans],max(x[2,!nans])))
	limz<-c(min(x[3,!nans],max(x[3,!nans])))
	
	plot3d(NaN,xlim=limx,ylim=limy,zlim=limz)
}
