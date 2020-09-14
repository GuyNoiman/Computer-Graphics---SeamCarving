# Computer-Graphics---SeamCarving
Project in Computer Graphics course -  implement and explore image resizing using the seam carving algorithm, along with other basic image processing operations.

FindSeams :
  In order to find a single seam we must perform the following steps :
    create Energy matrix (Calculated according to the level of variance between the indices)
    Create Cost matrix (Each index indicates a "damage assessment" or difference in the image after removal)
    Find path (The route with the lowest sum value in the cost matrix)
    Remove seam

ShowSeams:
  In order to display the removed seams, while running findseam we will update a matrix that will save the removed indices from the original image ("seamMapping").
  When we want to display the image with the removed seams - We will go over the "seamMapping" matrix and replace any removed index with a red color pixel.

